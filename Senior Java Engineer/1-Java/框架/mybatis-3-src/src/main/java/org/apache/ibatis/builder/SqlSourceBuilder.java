/*
 *    Copyright 2009-2021 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

/**
 * @author Clinton Begin
 */
public class SqlSourceBuilder extends BaseBuilder {

  private static final String PARAMETER_PROPERTIES = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

  public SqlSourceBuilder(Configuration configuration) {
    super(configuration);
  }

  public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
//    参数映射处理器ParameterMappingTokenHandler，处理SQL中的#{}参数占位符
    ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
//    Token解析器，用来解析#{}参数
    GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
    String sql;
//      调用GenericTokenParser#parse()将#{}参数转换为？占位符
    if (configuration.isShrinkWhitespacesInSql()) {
//      解析#{}
      sql = parser.parse(removeExtraWhitespaces(originalSql));
    } else {
      sql = parser.parse(originalSql);
    }
    return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
  }

  public static String removeExtraWhitespaces(String original) {
    StringTokenizer tokenizer = new StringTokenizer(original);
    StringBuilder builder = new StringBuilder();
    boolean hasMoreTokens = tokenizer.hasMoreTokens();
    while (hasMoreTokens) {
      builder.append(tokenizer.nextToken());
      hasMoreTokens = tokenizer.hasMoreTokens();
      if (hasMoreTokens) {
        builder.append(' ');
      }
    }
    return builder.toString();
  }

  private static class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {

    private final List<ParameterMapping> parameterMappings = new ArrayList<>();
    private final Class<?> parameterType;
    private final MetaObject metaParameters;

    public ParameterMappingTokenHandler(Configuration configuration, Class<?> parameterType, Map<String, Object> additionalParameters) {
      super(configuration);
      this.parameterType = parameterType;
      this.metaParameters = configuration.newMetaObject(additionalParameters);
    }

    public List<ParameterMapping> getParameterMappings() {
      return parameterMappings;
    }

    @Override
    public String handleToken(String content) {
      parameterMappings.add(buildParameterMapping(content));
      //替换为？ 占位符
      return "?";
    }

    private ParameterMapping buildParameterMapping(String content) {
//      将占位符内容转换为Map对象
      Map<String, String> propertiesMap = parseParameterMapping(content);
//      property对应的值为参数占位符名称
      String property = propertiesMap.get("property");
      Class<?> propertyType;
//如果内置参数或bind标签绑定的参数包含该属性，则参数类型为Getter方法的返回值类型
      if (metaParameters.hasGetter(property)) {
        propertyType = metaParameters.getGetterType(property);
//        判断参数类是否在注册了TypeHandler，如果注册了，则使用参数类型
      } else if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
        propertyType = parameterType;
//        如果指定了JdbcType，并且为CURSOR类型，则使用ResultSet
      } else if (JdbcType.CURSOR.name().equals(propertiesMap.get("jdbcType"))) {
        propertyType = java.sql.ResultSet.class;
//        如果参数类型为Map接口的子类型，则使用Object类型
      } else if (property == null || Map.class.isAssignableFrom(parameterType)) {
        propertyType = Object.class;
      } else {
//        获取parameterType对应的MetaClass对象，方便获取参数类型的反射信息
        MetaClass metaClass = MetaClass.forClass(parameterType, configuration.getReflectorFactory());
        if (metaClass.hasGetter(property)) {
          propertyType = metaClass.getGetterType(property);
        } else {
          propertyType = Object.class;
        }
      }
//      使用建造者模式，创建ParameterMapping对象
      ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, propertyType);
      Class<?> javaType = propertyType;
      String typeHandlerAlias = null;
      for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
        String name = entry.getKey();
        String value = entry.getValue();
//        指定ParameterType对象的属性
        if ("javaType".equals(name)) {
          javaType = resolveClass(value);
          builder.javaType(javaType);
        } else if ("jdbcType".equals(name)) {
          builder.jdbcType(resolveJdbcType(value));
        } else if ("mode".equals(name)) {
          builder.mode(resolveParameterMode(value));
        } else if ("numericScale".equals(name)) {
          builder.numericScale(Integer.valueOf(value));
        } else if ("resultMap".equals(name)) {
          builder.resultMapId(value);
        } else if ("typeHandler".equals(name)) {
          typeHandlerAlias = value;
        } else if ("jdbcTypeName".equals(name)) {
          builder.jdbcTypeName(value);
        } else if ("property".equals(name)) {
          // Do Nothing
        } else if ("expression".equals(name)) {
          throw new BuilderException("Expression based parameters are not supported yet");
        } else {
          throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content + "}.  Valid properties are " + PARAMETER_PROPERTIES);
        }
      }
      if (typeHandlerAlias != null) {
        builder.typeHandler(resolveTypeHandler(javaType, typeHandlerAlias));
      }
      return builder.build();
    }

    private Map<String, String> parseParameterMapping(String content) {
      try {
        return new ParameterExpression(content);
      } catch (BuilderException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new BuilderException("Parsing error was found in mapping #{" + content + "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
      }
    }
  }

}
