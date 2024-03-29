package com.minfo.mgr;

import java.util.List;

import com.minfo.dao.UserDAO;
import com.minfo.model.User;

public class UserManager {
	private UserDAO userDAO;

	public void setUserDAO(UserDAO userDAO) {
		this.userDAO = userDAO;
	}

	
	public void updateUser(User user) {
		userDAO.updateUser(user);
	}
	public void addUser(User user) {
		userDAO.addUser(user);
	}
	
	public List<User> getUser() {
		return userDAO.getUser();
	}
	

	public User getUser(Long id) {
		User u = userDAO.getUser(id);
		//System.out.println("user:"+u);
	//	System.out.println("answers:"+u.getUserAnswers());
		return userDAO.getUser(id);
	}
	
	public void removeUser(Long id) {
		userDAO.removeUser(id);
	}
	
	public User addRegisterUser() {
		User u = new User();
		userDAO.addUser(u);
		u.setUsername("u"+u.getId());
		u.setPassword("password");
		userDAO.updateUser(u);
		return u;
	}
}
