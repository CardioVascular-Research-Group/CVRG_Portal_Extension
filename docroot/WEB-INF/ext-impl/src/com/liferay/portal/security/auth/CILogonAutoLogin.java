/*
Copyright 2011 Johns Hopkins University Institute for Computational Medicine

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
/**
* @author Chris Jurado
* 
*/
package com.liferay.portal.security.auth;

import java.util.Calendar;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.StackTraceUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PrefsPropsUtil;
import com.liferay.portal.util.PropsValues;
import com.liferay.util.PwdGenerator;

public class CILogonAutoLogin implements AutoLogin {

	private static Log _log = LogFactoryUtil.getLog(CILogonAutoLogin.class);
	private long companyId; 

	public String[] login(HttpServletRequest req, HttpServletResponse res)
			throws AutoLoginException {
		
		String[] credentials = null;
		String userEmail = "";
		String[] userName;
		User user;
		
		_log.info("CILogon Extension");
		
		try {	
			companyId = PortalUtil.getCompanyId(req);

			if (!PrefsPropsUtil.getBoolean(
					companyId, PropsKeys.CILOGON_AUTH_ENABLED,
					PropsValues.CILOGON_AUTH_ENABLED)) {
				return credentials;
			}
			
			userEmail = getEmailFromCookie(req.getCookies());
			userName = getNameFromCookie(req.getCookies());
			if(userName == null){
				_log.error("Invalid or missing user name information from CILogon");
				userName = new String[]{"",""};
			}

			if (userEmail.equals("") || userEmail.length() < 1) {
				_log.error("Invalid or missing user login information from CILogon");
				return credentials;
			}

			credentials = new String[3];

			try{
				user = UserLocalServiceUtil.getUserByEmailAddress(PortalUtil.getCompany(req).getCompanyId(), userEmail);
			}catch (NoSuchUserException e) {
				_log.error("No user found to match " + userEmail);
				user = createNewUser(userEmail, userName);
			}
			
			
			credentials[0] = String.valueOf(user.getUserId());
			credentials[1] = user.getPassword();
			credentials[2] = Boolean.TRUE.toString();

			
		} catch (Exception e) {
			_log.error(StackTraceUtil.getStackTrace(e));
			throw new AutoLoginException(e);
		}

		return credentials;
	}
	
	private String getEmailFromCookie(Cookie[] cookies){
		
		String email = "";
		
		if(cookies != null){
			for (Cookie ck : cookies) {
				if ("CILOGON-USER_EMAIL".equals(ck.getName()) && !ck.getValue().equals("")) {
					_log.info("User Login received:" + ck.getValue());
					email = ck.getValue();
				}
		  	}
		}
		
		return email;
	}
	
	private String[] getNameFromCookie(Cookie[] cookies){
		
		String name = "";
		
		if(cookies != null){
			for (Cookie ck : cookies) {
				if ("CILOGON-USER_NAME".equals(ck.getName()) && !ck.getValue().equals("")) {
					name = (String)ck.getValue();
				}
			}
			return name.split(" ");
		}

		return null;
	}
	
	private User createNewUser(String userEmail, String[] userName){
		
		_log.info("Creating new user account for " + userName[0] + " " + userName[1]);
		
		User creatingUser = null;
		User newUser = null;
		try {
			creatingUser = UserLocalServiceUtil.getUserByEmailAddress(companyId, "test@liferay.com");
				
			UserLocalServiceUtil.addUser(creatingUser.getUserId(), companyId, false, "test", "test", false, (userName[0] + userName[1]), userEmail, 0L, "", Locale.US,
					userName[0], "", userName[1], 0, 0, false, 0, 1,1970, "User", null, null, null,
					null, false, new ServiceContext());
		
		} catch (PortalException e) {

			e.printStackTrace();
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return newUser;
		
	}

 }