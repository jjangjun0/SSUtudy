package com.example.ssutudy;
/* firebase 연동 - 자바 파일 */
public class UserAccountInfo {
    private String idTokens;
    private String emailId;
    private String password;
    private String role;
    private String teacherId;

    public UserAccountInfo(){}

    //    public UserAccountInfo(String idTokens, String emailId, String password, String role){
//        this.idTokens = idTokens;
//        this.emailId = emailId;
//        this.password = password;
//        this.role = role;
//    }
    public UserAccountInfo(String idTokens, String emailId, String password, String role,String teacherId){
        this.idTokens = idTokens;
        this.emailId = emailId;
        this.password = password;
        this.role = role;
        this.teacherId = teacherId;
    }


    public String getIdTokens() {
        return idTokens;
    }

    public String getEmailId() {
        return emailId;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public String getTeacherId(){ return teacherId; }


//    public void setIdTokens(String idTokens) {
//        this.idTokens = idTokens;
//    }
//
//    public void setEmailId(String emailId) {
//        this.emailId = emailId;
//    }
//
//    public void setPassword(String password) {
//        this.password = password;
//    }
//
//    public void setRole(String role) {
//        this.role = role;
//    }

}
