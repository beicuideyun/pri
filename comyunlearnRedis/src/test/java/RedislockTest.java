public class RedislockTest {
    public static void main(String[] args){
        for(int i = 0; i < 9; i++){
            new Thread(new Runnable() {
                public void run() {
                    RedisConnection redisConnection = RedisConnectionUtil.create();
                    LockServiceRdisImpl lockServiceRdis = new LockServiceRdisImpl();
                    lockServiceRdis.setRedisConnection(redisConnection);
                    lockServiceRdis.setDbIndex(15);
                    lockServiceRdis.setLockExpirseTIme(20);
                    String key = "20190501";
                    String value = lockServiceRdis.lock(key);
                    try{
                        if(value != null){
                            System.out.println(Thread.currentThread().getName() + " lock key = " + key + " success! ");
                            Thread.sleep(2 * 1000);
                        }else{
                            System.out.println(Thread.currentThread().getName() + " lock key = " + key + " failure! ");
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        if(value == null){
                            value = "";
                        }
                        System.out.println(Thread.currentThread().getName() + "unlock key = " + key + " " + lockServiceRdis.unLock(key, value));
                    }
                }
            }).start();
        }
    }
}
