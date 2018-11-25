package com.ddv.test.demo;

public abstract class Aaaa<T extends Object> {

	public abstract String sayHello(String aName);

	public abstract <U extends Object> U sayHelloObj(T aName);

}
