package sd2526.trab.server.java;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.api.java.Users;
import sd2526.trab.server.persistence.Hibernate;

public class JavaUsers implements Users {

    private static final String USER_TABLE_SELECT = "Select u FROM User u";

    private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

    private Hibernate hibernate;
    private final String domain;

    public JavaUsers(String domain) {
        this.domain = domain;
        hibernate = Hibernate.getInstance();
    }

    @Override
    public Result<String> postUser(User user) {
        Log.info("createUser : " + user);

        // Check if user data is valid
        if (user.getName() == null || user.getPwd() == null || user.getDisplayName() == null
                || user.getDomain() == null) {
            Log.info("User object invalid.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        if(!user.getDomain().equalsIgnoreCase(this.domain)){
            return Result.error(ErrorCode.FORBIDDEN);
        }

        User exiUser = hibernate.get(User.class,user.getName());
        if(exiUser!=null){
            if(exiUser.equals(user)) {
                return Result.ok(user.getName()+"@"+user.getDomain());
            }else{
                Log.info("User already exists with different data.");
                return Result.error(ErrorCode.CONFLICT);
            }
        }

        try {
            hibernate.persist(user);
        } catch (Exception e) {
            Log.info("DB error.");
            return Result.error(ErrorCode.CONFLICT);
        }
        return Result.ok(user.getName()+"@"+user.getDomain());
    }

    @Override
    public Result<User> getUser(String userId, String password) {
        Log.info("getUser : user = " + userId + "; pwd = " + password);

        // Check if user is valid
        if (userId == null || password == null) {
            Log.info("UserId or password null.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        User user = null;
        try {
            user = hibernate.get(User.class, userId);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }

        // Check if user exists and password is correct...
        if (user == null || !user.getPwd().equals(password)) {
            Log.info("Password is incorrect");
            return Result.error(ErrorCode.FORBIDDEN);
        }

        return Result.ok(user);

    }

    @Override
    public Result<User> updateUser(String name, String password, User info) {

        if(info == null) return Result.error(ErrorCode.BAD_REQUEST);

        //o metodo get ja trata do forbidden e bad_request
       var res = getUser(name,password);
       if(!res.isOK()) return res;

       User user = res.value();

       if(info.getName() != null && !info.getName().equals(user.getName())){
           Log.info("Attempted to change name.");
           return Result.error(ErrorCode.BAD_REQUEST);
       }
        if(info.getDomain() != null && !info.getDomain().equals(user.getDomain())){
            Log.info("Attempted to change domain.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

       //se algum valor do info == null, n se altera
        if(info.getPwd()!=null) user.setPwd(info.getPwd());
        if(info.getDisplayName()!=null)user.setDisplayName(info.getDisplayName());

        try{
            hibernate.update(user);
        }catch (Exception e){
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(user);
    }

    @Override
    public Result<User> deleteUser(String name, String password) {

        var res = getUser(name,password);
        if(!res.isOK()) return res;


        User user = res.value();


        try{
            hibernate.delete(user);

            new Thread(()->{
                //Messages server para apagar inbox, quando implementarmos

            }).start();

            return Result.ok(user);

        }catch (Exception e){
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {

        if (query == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        var res = getUser(name,pwd);
        if(!res.isOK()) return Result.error(res.error());

        try{
            List<User> users = hibernate.jpql(USER_TABLE_SELECT,User.class);

            String qry = query.toLowerCase();

            //String qry = (query  == null) ? "" : query.toLowerCase();

            List<User> userHits = new ArrayList<>();

            for(User u : users){

                if(u.getName().toLowerCase().contains(qry)){
                    //para n passar o user verdadeiro
                    User copy = new User(u.getName(),"",u.getDisplayName(),u.getDomain());
                    userHits.add(copy);
                }
            }

            return Result.ok(userHits);

        }catch (Exception e){
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }
}
