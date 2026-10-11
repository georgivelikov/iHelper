package com.ihelper;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IhelperApplication {

	public static void main(String[] args) {
		// Everything is UTC. Also keeps `java -jar` working on hosts whose zone name PostgreSQL rejects
		// (e.g. Windows' legacy "Europe/Kiev").
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(IhelperApplication.class, args);
	}

}
