package com.jc.compute;

import java.util.Stack;

public interface RuleAction<T> {

	public void run(T lastValue, Stack<Snapshot<T>> history, String message, String severity, String alertType, boolean sendEmail);
	public String namespace();
}
