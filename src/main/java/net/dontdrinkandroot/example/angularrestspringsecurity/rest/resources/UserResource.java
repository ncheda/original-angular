package net.dontdrinkandroot.example.angularrestspringsecurity.rest.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.sun.org.apache.xalan.internal.xslt.Process;
import net.dontdrinkandroot.example.angularrestspringsecurity.dao.user.JpaUserDao;
import net.dontdrinkandroot.example.angularrestspringsecurity.dao.user.UserDao;
import net.dontdrinkandroot.example.angularrestspringsecurity.entity.User;
import net.dontdrinkandroot.example.angularrestspringsecurity.rest.TokenUtils;
import net.dontdrinkandroot.example.angularrestspringsecurity.transfer.RegistroDns;
import net.dontdrinkandroot.example.angularrestspringsecurity.transfer.ResultTransfer;
import net.dontdrinkandroot.example.angularrestspringsecurity.transfer.TokenTransfer;
import net.dontdrinkandroot.example.angularrestspringsecurity.transfer.UserTransfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
@Path("/user")
public class UserResource
{

	@Autowired
	private UserDao userService;

	@Autowired
	@Qualifier("authenticationManager")
	private AuthenticationManager authManager;

	@Autowired
	private PasswordEncoder passwordEncoder;
	/**
	 * Retrieves the currently logged in user.
	 * 
	 * @return A transfer containing the username and the roles.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public UserTransfer getUser()
	{
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Object principal = authentication.getPrincipal();
		if (principal instanceof String && ((String) principal).equals("anonymousUser")) {
			throw new WebApplicationException(401);
		}
		UserDetails userDetails = (UserDetails) principal;

		return new UserTransfer(userDetails.getUsername(), this.createRoleMap(userDetails));
	}


	/**
	 * Authenticates a user and creates an authentication token.
	 * 
	 * @param user
	 *            the user.
	 * @return A transfer containing the authentication token.
	 */
	@Path("authenticate")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public TokenTransfer authenticate(User user)
	{
		UsernamePasswordAuthenticationToken authenticationToken =
				new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
		Authentication authentication = this.authManager.authenticate(authenticationToken);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		/*
		 * Reload user as password of authentication principal will be null after authorization and
		 * password is needed for token generation
		 */
		UserDetails userDetails = this.userService.loadUserByUsername(user.getUsername());

		return new TokenTransfer(TokenUtils.createToken(userDetails));
	}

	@Path("register")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public TokenTransfer register(User user)
	{
		User userUser = new User(user.getName(), this.passwordEncoder.encode(user.getPassword()));
		userUser.addRole("admin");
		userUser.addRole("user");
		this.userService.save(userUser);
		UserDetails userDetails = this.userService.loadUserByUsername(user.getUsername());
		return new TokenTransfer(TokenUtils.createToken(userDetails));
	}


	private Map<String, Boolean> createRoleMap(UserDetails userDetails)
	{
		Map<String, Boolean> roles = new HashMap<String, Boolean>();
		for (GrantedAuthority authority : userDetails.getAuthorities()) {
			roles.put(authority.getAuthority(), Boolean.TRUE);
		}

		return roles;
	}


	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{id}")
	public ResultTransfer command(@PathParam("id") Long id) {
		//String[] cmd = {"cat /usr/share/tomcat/conf/tomcat-users.xml"};
		ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", " awk '{printf(\"%s %s %d\\n\", $1, $4, NR)}' /var/lib/named/master/armada.mil.uy.zone | grep  '^[a-z][a-z][a-z][a-z0-9][a-z0-9][0-9][0-9].*$' | sed 's/.armada.mil.uy.//g' | sort");
		java.lang.Process p = null;
		pb.redirectErrorStream(true);
		try {
			p = pb.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String line;
		BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		ArrayList<RegistroDns> salida = new ArrayList<RegistroDns>();
		try {
			while ((line = input.readLine()) != null) {
				line = line.trim();
				if(!line.equals("")){
					System.out.println(line);
					ArrayList<String> campos =new ArrayList<String>();
					String[] x = line.split("\\s+");
					for(int i=0; i<x.length && i <3;i++){
						campos.add(x[i]);
					}
					if(x.length>=2 && !x[0].equals("") && !x[1].equals("") ){
						salida.add(new RegistroDns(x[0],x[1], campos));
					}
				}
			}
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ResultTransfer(salida);
	}
}