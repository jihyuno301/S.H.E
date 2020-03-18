package com.so_mini.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.transaction.Transactional;


import com.so_mini.domain.Account;
import com.so_mini.domain.Token;
import com.so_mini.jwt.CustomUserDetails;
import com.so_mini.jwt.JwtTokenUtil;
import com.so_mini.repository.AccountRepository;
import com.so_mini.service.JwtUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin
@RequestMapping // This means URL's start with /demo (after Application path)
public class UserController {

	private Logger logger = LoggerFactory.getLogger(ApplicationRunner.class);
	@Autowired
	private AccountRepository accountRepository;
	@Autowired
	RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private JwtUserDetailsService userDetailsService;
	@Autowired
	private JwtTokenUtil jwtTokenUtil;
	@Autowired
	private AuthenticationManager am;
	@Autowired
	private PasswordEncoder bcryptEncoder;

	@PostMapping(path = "/public/signin")
	public Map<String, Object> login(@RequestBody Map<String, String> m) throws Exception {
		final String nickname = m.get("nickname");
		System.out.println("닉네임:"+ m.get("nickname"));
		logger.info("test input username: " + nickname);
		try {
			System.out.println("여기에러야1");
			am.authenticate(new UsernamePasswordAuthenticationToken(nickname, m.get("password")));
			System.out.println("여기에러야2");
		} catch (Exception e){
			System.out.println("여기에러야??");
			throw e;
		}
		System.out.println("여기까지성공?1");
		final CustomUserDetails userDetails = userDetailsService.loadUserByUsername(nickname);
		System.out.println("여기까지성공?2");
		final String accessToken = jwtTokenUtil.generateAccessToken(userDetails);
		System.out.println("여기까지성공?3");
		final String refreshToken = jwtTokenUtil.generateRefreshToken(nickname);

		Token retok = new Token();
		retok.setUsername(nickname);
		retok.setRefreshToken(refreshToken);

		//generate Token and save in redis
		ValueOperations<String, Object> vop = redisTemplate.opsForValue();
		vop.set(nickname, retok);

		logger.info("generated access token: " + accessToken);
		logger.info("generated refresh token: " + refreshToken);
		Map<String, Object> map = new HashMap<>();
		map.put("accessToken", accessToken);
		map.put("refreshToken", refreshToken);
		return map;
	}


	@PostMapping(path = "/public/signup") // Map ONLY POST Requests
	public Map<String, Object> addNewUser(@RequestBody Account account) {
		// @ResponseBody means the returned String is the response, not a view name
		// @RequestParam means it is a parameter from the GET or POST request
		String un = account.getNickname();
		Map<String, Object> map = new HashMap<>();
		System.out.println("회원가입요청 아이디: " + un + "비번: " + account.getPassword());
		if (accountRepository.findByNickname(un) == null) {
			account.setPassword(bcryptEncoder.encode(account.getPassword()));
			account.setGrade("1");
			map.put("success", true);
			System.out.println("여기까지 성공?");
			accountRepository.save(account);
			System.out.println("여기까지 성공?2 ");
			return map;
		} else {
			map.put("success", false);
			map.put("message", "duplicated username");
		}
		return map;
	}


}