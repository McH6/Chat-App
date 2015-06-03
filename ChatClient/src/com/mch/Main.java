package com.mch.controller;


import com.mch.config.Config;
import com.mch.view.ClientGUI;


public class Main {

	public static void main(String[] args) {
		new ClientGUI(Config.DEFAULT_HOST, Config.DEFAULT_PORT);
	}
	
}
