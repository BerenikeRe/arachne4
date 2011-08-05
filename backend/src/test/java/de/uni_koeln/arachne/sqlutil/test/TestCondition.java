package de.uni_koeln.arachne.sqlutil.test;

import static org.junit.Assert.*;

import org.junit.*;

import de.uni_koeln.arachne.sqlutil.Condition;



public class TestCondition {

	@Test
	public void testCondition(){
		Condition toTest1 =  new Condition();
		
		toTest1.setPart1("`bauwerk`.`PS_BauwerkID`");
		toTest1.setPart2("100");
		toTest1.setOperator("=");
		
		assertEquals(toTest1.toString(), " `bauwerk`.`PS_BauwerkID` = 100");
		
		Condition toTest2 =  new Condition();
		
		toTest2.setPart1("`bauwerk`.`kurzbeschreibungBauwerk`");
		toTest2.setPart2("\"%Tempel%\"");
		toTest2.setOperator("LIKE");
		
		assertEquals(toTest2.toString(), " `bauwerk`.`kurzbeschreibungBauwerk` LIKE \"%Tempel%\"");
	}
	
	
}
