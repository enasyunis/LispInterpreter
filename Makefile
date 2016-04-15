# AUTHOR:	ENAS YUNIS
target: clean build run

build:
	javac -g -cp . *.java
	jar cfe LispSysP2.jar InterpreterP2  *.class
	$(RM) *.class

run:
	java -jar LispSysP2.jar

clean:
	$(RM) *.class *.jar
