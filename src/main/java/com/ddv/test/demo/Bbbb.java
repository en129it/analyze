package com.ddv.test.demo;

import java.io.Serializable;

public class Bbbb extends Aaaa<Number> implements Cccc, Serializable {

	@Override
	public Dddd sayHelloAgain(String aName) {
		return new Dddd();
	}

	@Component("Hello")
	@Override
	public String sayHello(String aName) {
		return "Hello " + aName;
	}

	@Override
	public Number sayHelloObj(Number aName) {
		return aName;
	}

	public static class MyInnerBbbb {
		public String goodBye() {
			return "";
		}
	}
}
