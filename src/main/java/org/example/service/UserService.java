package org.example.service;

import org.example.dao.UserDAO;
import org.example.model.User;
import org.mindrot.jbcrypt.BCrypt;

public class UserService {
    private final UserDAO userDao;
    private final EmailService emailService;

    public UserService(UserDAO userDao, EmailService emailService) {
        this.userDao = userDao;
        this.emailService=emailService;
    }

    public User registerUser(String username, String email, String password) throws InvalidDataException{
           if (username==null || username.trim().isEmpty() ||
               email==null || email.trim().isEmpty() ||
               password==null || password.trim().isEmpty()){
               throw new InvalidDataException("Datele introduse nu sunt valide!");
           }

           if(!email.contains("@") || !email.contains(".")){
               throw new InvalidDataException("Datele introduse nu sunt valide!");
           }

           User existingUser=userDao.findByEmail(email);
           if(existingUser!=null){
               throw new InvalidDataException("Exista deja un cont inregistrat cu acest email!");
           }

        if (password == null || password.length() < 6) {
            throw new InvalidDataException("Parola trebuie să conțină cel puțin 6 caractere!");
        }

           String hashedPassword=BCrypt.hashpw(password, BCrypt.gensalt());

           User newUser= new User(email, username, hashedPassword);
           userDao.save(newUser);

           return newUser;
    }

    public User loginUser(String email, String password) throws InvalidDataException{
          if(email==null || password==null){
              throw new InvalidDataException("Email-ul si parola sunt obligatorii pentru autentificare!");
          }

          User user=userDao.findByEmail(email);
          if(user==null || !BCrypt.checkpw(password,user.getPassword())){
              throw new InvalidDataException("Email sau parola incorecta!");
          }
          return user;
    }

    public void deleteAccount(User user, String confirmedPassword) throws InvalidDataException {

        if (!BCrypt.checkpw(confirmedPassword, user.getPassword())) {
            throw new InvalidDataException("Parola incorectă! Nu poți șterge contul.");
        }

        String randomFictiv = java.util.UUID.randomUUID().toString();

        user.setUsername("Cont Șters");
        user.setEmail("sters_" + randomFictiv.substring(0, 8) + "@sters.com");

        user.setPassword(BCrypt.hashpw(randomFictiv, BCrypt.gensalt()));

        userDao.update(user);
    }

    public void resetPassword(String token, String newPassword) throws InvalidDataException {

        User user = userDao.findByResetToken(token);

        if (user == null) {
            throw new InvalidDataException("Link-ul de resetare este invalid sau a fost deja folosit!");
        }

        if (java.time.LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            throw new InvalidDataException("Link-ul de resetare a expirat! Te rugăm să ceri altul.");
        }

        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userDao.update(user);
    }

    public void initiatePasswordReset(String email) throws InvalidDataException{
        User user = userDao.findByEmail(email);

        if (user != null) {
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(15));

            userDao.update(user);
            emailService.sendPasswordResetEmail(user.getEmail(), token);

        } else {
             throw new InvalidDataException("User-ul cu email-ul "+email+" nu exista!");
        }
    }
}
