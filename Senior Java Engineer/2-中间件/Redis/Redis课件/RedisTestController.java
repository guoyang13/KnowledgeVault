import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Controller
public class RedisTestController {

     @Autowired
     public StringRedisTemplate stringRedisTemplate;

     @RequestMapping("likeBlog")
     @ResponseBody
     public String likeBlog(String blogId,String userId){
          String key="blog:"+blogId;
          Boolean isMember =  stringRedisTemplate.opsForSet().isMember(key,userId);
          if(BooleanUtils.isFalse(isMember)){
               stringRedisTemplate.opsForSet().add(key,userId);
          }else{
               stringRedisTemplate.opsForSet().remove(key,userId);
          }
          return "SUCCESS";
     }

     @RequestMapping("getBlog")
     @ResponseBody
     public Blog getBlog(String blogId,String userId){
          String key="blog:"+blogId;
          Blog blog  = new Blog();
          blog.setTitle("测试");
          blog.setLikeCount(stringRedisTemplate.opsForSet().size(key));
          blog.setIsLike(stringRedisTemplate.opsForSet().isMember(key,userId));
          blog.setAuthor("亚风");
          blog.setTime(new Date());
          blog.setId("1");
          blog.setContent("正文内容");
          return blog;
     }

     @RequestMapping("radius")
     @ResponseBody
     public List<GeoResult<RedisGeoCommands.GeoLocation<String>>> radius(double x,double y){
          RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                  .limit(5)
                  //包含成员距离中心点的距离
                  .includeDistance();

          GeoResults<RedisGeoCommands.GeoLocation<String>> result = stringRedisTemplate.opsForGeo().radius("g2",
                  new Circle(new Point(x,y),new Distance(10, Metrics.KILOMETERS)),args);
          return result.getContent();
     }


     @RequestMapping("sign")
     @ResponseBody
     public  String sign(String userId) {
          LocalDateTime now = LocalDateTime.now();
          String key= "sign:"+userId+ ":"+now.format(DateTimeFormatter.ofPattern("yyyyMM"));
          stringRedisTemplate.opsForValue().setBit(key,now.getDayOfMonth()-1,true);
          return "SUCCESS";
     }

     @RequestMapping("signCount")
     @ResponseBody
     public Long signCount(String userId) {
          LocalDateTime now = LocalDateTime.now();
          String key= "sign:"+userId+ ":"+now.format(DateTimeFormatter.ofPattern("yyyyMM"));
          List<Long> result = stringRedisTemplate.opsForValue().bitField(key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.
                  unsigned(now.getDayOfMonth())).valueAt(0));
          if(CollectionUtils.isEmpty(result)){
               return 0L;
          }
          Long num = result.get(0);
          Long count = 0L;
          while(true){
               if((num&1)==0){
                    break;
               }else{
                    count++;
               }
               num >>>=1;
          }
          return count;
     }

}
