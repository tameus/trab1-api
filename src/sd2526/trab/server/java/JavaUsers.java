package sd2526.trab.server.java;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.api.java.Users;
import sd2526.trab.server.persistence.Hibernate;

public class JavaUsers implements Users {
    private static final String USER_TABLE_SELECT = "SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER('%%%s%%')";
    private static final String SELECT_USER_INBOX = "SELECT i FROM Inbox i WHERE i.userAddress = '%s'";

    private static Logger Log = Logger.getLogger(JavaUsers.class.getName());
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    private Hibernate hibernate;
    private final String domain;

    public JavaUsers(String domain) {
        this.domain = domain;
        hibernate = Hibernate.getInstance();
    }

    @Override
    public Result<String> postUser(User user) {
        Log.info("postUser");

        // Check if user data is valid
        if (user==null||user.getName() == null || user.getPwd() == null || user.getDisplayName() == null || user.getDomain() == null) {
            Log.info("User object invalid.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        if(!user.getDomain().equalsIgnoreCase(this.domain)){
            return Result.error(ErrorCode.FORBIDDEN);
        }
        String userId = user.getName() + "@" + user.getDomain();
        synchronized (user.getName().intern()) {
            try {
                User exiUser = hibernate.get(User.class, user.getName());
                if (exiUser != null) {
                    if (exiUser.equals(user)) {
                        return Result.ok(userId);
                    } else {
                        Log.info("User already exists with different data.");
                        return Result.error(ErrorCode.CONFLICT);
                    }
                }
                hibernate.persist(user);
                return Result.ok(userId);
            } catch (Exception e) {
                Log.info("DB error.");
                return Result.error(ErrorCode.INTERNAL_ERROR);
            }
        }

    }

    @Override
    public Result<User> getUser(String name, String password) {
        Log.info("getUser : user = " + name + "; pwd = " + password);

        //utilizador valido
        if (name == null || password == null) {
            Log.info("UserId or password null.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        try {
            User user = hibernate.get(User.class, name);
            //utilizador valido e pass correta
            if (user == null || !user.getPwd().equals(password)) {
                Log.info("Password is incorrect");
                return Result.error(ErrorCode.FORBIDDEN);
            }
            return Result.ok(user);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }



    }

    @Override
    public Result<User> updateUser(String name, String password, User info) {

        if(info == null) return Result.error(ErrorCode.BAD_REQUEST);
        //o metodo get ja trata do forbidden e bad_request

        synchronized (name.intern()) {
           Result<User> res = getUser(name,password);
           if (!res.isOK()) return res;

           User user = res.value();

           if (info.getName() != null && !info.getName().isEmpty() && !info.getName().equals(user.getName())) {
               Log.info("Attempted to change name.");
               return Result.error(ErrorCode.BAD_REQUEST);
           }
           if (info.getDomain() != null && !info.getDomain().isEmpty() && !info.getDomain().equals(user.getDomain())) {
               Log.info("Attempted to change domain.");
               return Result.error(ErrorCode.BAD_REQUEST);
           }

           //se algum valor do info == null, n se altera
           //se é igual nao é preciso alterar
           boolean needsUpdate = false;
           if (info.getPwd() != null && !info.getPwd().isEmpty() && !info.getPwd().equals(user.getPwd())) {
               user.setPwd(info.getPwd());
               needsUpdate = true;
           }
           if (info.getDisplayName() != null && !info.getDisplayName().isEmpty() && !info.getDisplayName().equals(user.getDisplayName())) {
               user.setDisplayName(info.getDisplayName());
               needsUpdate = true;
           }

           try {
               if (needsUpdate) {
                   hibernate.update(user);
               }
               return Result.ok(user);
           } catch (Exception e) {
               return Result.error(ErrorCode.INTERNAL_ERROR);
           }
       }
    }

    @Override
    public Result<User> deleteUser(String name, String password) {
        User user;
        synchronized (name.intern()) {
            Result<User> res = getUser(name, password);
            if (!res.isOK()) return res;
            user = res.value();
            try {
                hibernate.delete(user);
            } catch (Exception e) {
                return Result.error(ErrorCode.INTERNAL_ERROR);
            }
        }

        //apagar inboxes
        threadPool.submit(() -> {
            try {
                String query = String.format(SELECT_USER_INBOX, name);
                List<Inbox> entries = hibernate.jpql(query, Inbox.class);
                for (Inbox i : entries) {
                    try {
                        hibernate.delete(i);
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                Log.warning("Failed to clean up inbox for deleted user " + name + ": " + e.getMessage());
            }
        });

        return Result.ok(user);
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        if (query == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        Result<User> res = getUser(name, pwd);
        if (!res.isOK()) return Result.error(res.error());
        try {
            String sql = String.format(USER_TABLE_SELECT, query);
            List<User> users = hibernate.jpql(sql, User.class);                
            List<User> userList = new ArrayList<>();
            //tirar pwd
            for (User u : users) {
                userList.add(new User(u.getName(), "", u.getDisplayName(), u.getDomain()));
            }
            return Result.ok(userList);

        } catch (Exception e) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }

    }

    @Override
    public Result<User> verifyUser(String name) {
        if (name == null || name.isBlank()){
            return Result.error(ErrorCode.BAD_REQUEST);
        }
        try {
            User user = hibernate.get(User.class, name);
            if (user == null){
                return Result.error(ErrorCode.NOT_FOUND);
            }
            //devolver user sem pwd
            return Result.ok(new User(user.getName(), "", user.getDisplayName(), user.getDomain()));
        } catch (Exception e) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }
}
