import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.text.DateFormat;
import java.util.Collections;
import java.util.UUID;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class LockServiceRdisImpl implements LockService {

    private static Logger log = LoggerFactory.getLogger((LockServiceRdisImpl.class));

    private RedisConnection redisConnection;

    private Integer dbIndex;

    private static String SET_SUCCESS = "OK";

    private DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    private Integer lockExpirseTIme;

    private Integer tryExpireseTime;

    private static String KEY_PRE = "REDIS_LOCK_";

    public void setRedisConnection(RedisConnection redisConnection){
        this.redisConnection = redisConnection;
    }

    public void setDbIndex(Integer dbIndex){
        this.dbIndex = dbIndex;
    }

    public void setLockExpirseTIme(Integer lockExpirseTIme){
        this.lockExpirseTIme = lockExpirseTIme;
    }

    public void setTryExpireseTime(Integer tryExpireseTime){
        this.tryExpireseTime = tryExpireseTime;
    }

    public String lock(String key){
        Jedis jedis = null;
        try {
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            key = KEY_PRE + key;
            String value = fetchLockValue();
            if(SET_SUCCESS.equals(jedis.set(key, value, SetParams.setParams().nx().ex(lockExpirseTIme)))){
                log.debug("Redis Lock key : " + key + ",value: " + value);
                return value;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(jedis != null){
                jedis.close();
            }
        }
        return null;
    }

    public String tryLock(String key){
        Jedis jedis = null;
        try{
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            key = KEY_PRE + key;
            String value = fetchLockValue();
            Long firstTryTime = new Date().getTime();
            do{
                if(SET_SUCCESS.equals(jedis.set(key, value, SetParams.setParams().nx().ex(lockExpirseTIme)))){
                    log.debug("Redis Lock key : " + key + ",value: " + value);
                    return value;
                }
                log.info("Redis lock failure, waiting try next");
                try{
                    Thread.sleep(100);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }while((new Date().getTime() - tryExpireseTime * 1000) < firstTryTime);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(jedis != null){
                jedis.close();
            }
        }
        return null;
    }

    public boolean unLock(String key, String value) {
        Long RELEASE_SUCCESS = 1L;
        Jedis jedis = null;
        try{
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            key = KEY_PRE + key;
            String command = "if redis.call('get', KEYS[1])==ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            if(RELEASE_SUCCESS.equals(jedis.eval(command, Collections.singletonList(key), Collections.singletonList(value)))){
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (jedis != null){
                jedis.close();
            }
        }
        return false;
    }

    private String fetchLockValue(){
        return UUID.randomUUID().toString() + "_" + df.format(new Date());
    }
}
