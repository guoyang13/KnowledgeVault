```Java
    main(){
        // UserService service = new UserServiceImpl();
        
        UserService service = getUserService("userService");
    }
    
    UserService getUserService(String name){
        if("userService".equals(name)){
            return new UserServiceImpl();
        }
    }
    
    Object getBean(String name){
        if("userService".equals(name)){
            return new UserServiceImpl();
        }else if("orderService".equals(name)){
            return new OrderServiceImpl();
        }else if ("goodsService".equals(name)){
            OrderService service = new OrderServiceImpl();
            service.setXXX("属性值");
            return service ;
        }
    }
    
    //开发人员
    //入行五年的开发人员
    public Object getBean(String name) {
        // 解决代码扩展性问题的一种方案就是使用外部的配置文件
        // java中的配置文件，一般就是xml和properties文件(XML)
        // <beans>
        //   <bean id="唯一标识" class="类的全路径" init-method="初始化方法名称" scope="singleton/prototype">
        //     <property name="类的简单属性名称" value="具体属性的值">
        //     <property name="类的简单属性名称" value="具体属性的值">
        //     <property name="类的引用属性名称" ref="具体对象的引用值">
        //   </bean>
        // </beans>
        
        // 创建对象的方式，除了new之外，还有一种就是反射。
        // 反射最核心的类就是Class类
        // Class clazz = Class.forName("类的全路径");
        // Constructor constructor = clazz.getDeclearConstructor();
        // Object bean = constructor.newInstance();
        // Field field = clazz.getDeclearField("类的属性名称");
        // field.set(bean,"具体属性的值");
        // Method method = clazz.getDeclearMethod("初始化方法名称",方法参数类型);
        // method.invoke(bean,方法参数值);

        // 设计一些类，用来保存bean标签的相关信息
        // BeanDefinition定义
        // String id
        // String className;
        // Class type;
        // String scope
        // String initMethod;
        // List<PropertyValue> propertyValues;

        // PropertyValue定义
        // String name;
        // Object value; //为了区分value的类型，需要再分别定义具体的值的对象来存储值

        // 存储简单类型的值对应的对象
        // TypedStringValue定义
        // String value;
        // Class type;

        // 存储引用类型的值对应的对象
        // RuntimeBeanReference定义
        // String ref; //ref具体的值，这个是另一个bean的唯一标识

        // 存储BeanDefinition信息的集合
        // Map<String,BeanDefinition>


        // 【BeanDefinition加载流程分析】：
        // xml中bean的相关信息，什么时候加载到内存中，是一次加载某一个bean配置还是一次性把整个xml都加载到内存中？
        // XML配置文件-->IO流-封装成DOM对象->DOM-->Dom4J --解析XML-- > BeanDefinition(封装一个bean标签的信息）
        // 一个XML文件中包含多个bean标签，所以我们一次性加载完xml之后，会产生很多BeanDefinition对象，需要找个容器来存储一下
        // 一般来说，Java中的容器首先就是Map(key,value)，key就是bean标签的唯一标识，value就是BeanDefinition对象

        
        // 存储Bean实例的集合
        // Map<String,Object>
        
        // 【Bean对象创建流程分析】
        // bean对象什么时候创建，是一次性把所有对象都创建出来，还是每次需要什么对象创建什么对象
        // 答案：需要的时候再去创建是最好的，这是懒加载方式。
        // 一个完整的bean对象创建，
        // 1、创建出来bean实例（此时还没有对属性进行赋值）
        // 2、对创建出来的bean实例进行属性赋值
        // 3、调用创建出来bean实例的初始化方法

        
        // 1、创建出来bean实例（此时还没有对属性进行赋值）

        // 2、对创建出来的bean实例进行属性赋值

        // 3、调用创建出来bean实例的初始化方法

        // 我们创建出来的bean对象，是循环使用呢？还是每次使用完就销毁，下次再重新创建呢？
        // bean对象一般分为两类：
        // 1、数据对象（有状态对象，可能不同的对象使用的数据不一样 ，所以可能需要重新创建）--prototype多例
        // 2、业务对象（无状态对象，可以重复使用）---singleton单例

        // 对于singleton单例的对象，我们也需要找一个容器来存储，方便后面长期使用。
        // 一般来说，Java中的容器首先就是Map(key,value)，key就是bean标签的唯一标识，value就是Bean对象


        return null;
    }
```