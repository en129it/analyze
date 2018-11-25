package com.ddv.test.demo;

public class Bbbb2 extends Bbbb {
	
	public Bbbb2() {
		super();
	}
	
	@Override
	public String sayHello(String aName) {
		super.sayHello(aName);
		int a = 45;
		a += 45;
		a += 45;
		a += 45;
		a += 45;
		a += 45;
		a += 45;
		a += 45;
				
		return super.sayHello(aName);
	}

	public static class MyInnerBbbb2 extends MyInnerBbbb {
		
		@Override
		public String goodBye() {
			// TODO Auto-generated method stub
			return super.goodBye();
		}
	}

	public static class MyInnerBbbb3 extends com.ddv.test.demo.Bbbb.MyInnerBbbb {
		
		@Override
		public String goodBye() {
			// TODO Auto-generated method stub
			return super.goodBye();
		}
	}

	public static class MyInnerBbbb4 extends Bbbb.MyInnerBbbb {
		
		@Override
		public String goodBye() {
			// TODO Auto-generated method stub
			return super.goodBye();
		}
	}
}
