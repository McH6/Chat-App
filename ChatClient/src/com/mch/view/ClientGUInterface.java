package com.mch.view;

import java.util.List;

public interface ClientGUInterface {

	public void append(String str);

	public void connectionFailed();

	public void setUsers(List<String> users);
}
