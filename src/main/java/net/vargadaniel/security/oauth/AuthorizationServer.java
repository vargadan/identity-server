package net.vargadaniel.security.oauth;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableAuthorizationServer
@EnableResourceServer
@RestController
public class AuthorizationServer {

	private static Logger log = LoggerFactory.getLogger(AuthorizationServer.class);

	public static void main(String[] args) {
		SpringApplication.run(AuthorizationServer.class, args);
	}
	
	@RequestMapping(value="/user", produces="application/json") 
	public Map<String, Object> user(OAuth2Authentication user) {
		log.info("Requesting user info : " + user);
		Map<String, Object> userInfo = new HashMap<>();
		userInfo.put("user", user.getUserAuthentication().getPrincipal());
		userInfo.put("authorities", AuthorityUtils.authorityListToSet(user.getUserAuthentication().getAuthorities()));
		return userInfo;
	}
	
	@Configuration
	@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
	protected static class WebSecurity extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(AuthenticationManagerBuilder auth) throws Exception {
			auth.inMemoryAuthentication().withUser("hello").password("bello").roles("USER", "ADMIN", "SUPERUSER");
		}

		@Override
		@Bean
		public AuthenticationManager authenticationManagerBean() throws Exception {
			return super.authenticationManagerBean();
		}

		@Override
		@Bean
		public UserDetailsService userDetailsServiceBean() throws Exception {
			return super.userDetailsServiceBean();
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests().anyRequest().authenticated();
		}
		
	}

	@Configuration
	protected static class OAuth2Config extends AuthorizationServerConfigurerAdapter {

		@Autowired
		AuthenticationManager authenticationManager;

		@Autowired
		UserDetailsService userDetailsService;

		@Override
		public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
			log.info("*************************************************************************");
			log.info("authenticationManager.class : " + authenticationManager.getClass().getName());
			log.info("userDetailsService.class : " + userDetailsService.getClass().getName());
			log.info("*************************************************************************");
			endpoints.authenticationManager(new AuthenticationManager() {

				@Override
				public Authentication authenticate(Authentication authentication) throws AuthenticationException {
					log.info("authentication : " + authentication.toString());
					log.info("credentials : " + authentication.getCredentials().toString());
					return authenticationManager.authenticate(authentication);
				}
			});
			endpoints.userDetailsService(new UserDetailsService() {

				@Override
				public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
					log.info("username : " + username);
					return userDetailsService.loadUserByUsername(username);
				}
			});
		}

		@Override
		public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
			super.configure(security);
		}

		@Override
		public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
			clients.inMemory().withClient("acme").secret("acme_secret")
					.authorizedGrantTypes("refresh_token", "password", "client_credentials").scopes("webclient");
		}
	}
//
//	@Component
//	protected static class UserDetailsServiceImpl implements UserDetailsService {
//
//		@Override
//		public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//			if ("hello".equals(username)) {
//				return new UserDetails() {
//
//					@Override
//					public Collection<? extends GrantedAuthority> getAuthorities() {
//						return Arrays.asList(new SimpleGrantedAuthority("ADMIN"),
//								new SimpleGrantedAuthority("SUPERUSER"), 
//								new SimpleGrantedAuthority("USER"));
//					}
//
//					@Override
//					public String getPassword() {
//						return "bello";
//					}
//
//					@Override
//					public String getUsername() {
//						return "hello";
//					}
//
//					@Override
//					public boolean isAccountNonExpired() {
//						return true;
//					}
//
//					@Override
//					public boolean isAccountNonLocked() {
//						return true;
//					}
//
//					@Override
//					public boolean isCredentialsNonExpired() {
//						return true;
//					}
//
//					@Override
//					public boolean isEnabled() {
//						return true;
//					}
//
//				};
//			}
//			throw new UsernameNotFoundException("User not found, username : " + "" + username);
//		}
//	}

}
