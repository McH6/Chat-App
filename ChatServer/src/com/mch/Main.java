package com.mch;

import com.mch.config.Config;
import com.mch.view.ServerGUI;


public class Main {

	public static void main(String[] args) {
		new ServerGUI(Config.DEFAULT_PORT);
	}
}

