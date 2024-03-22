package miniJava;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class Compiler {
	// Main function, the file to compile will be an argument.
	public static void main(String[] args) {
		// TODO: Instantiate the ErrorReporter object
		ErrorReporter _errors = new ErrorReporter();
		// TODO: Check to make sure a file path is given in args
		try {
			if (args.length > 0) {
				File source = new File(args[0]);
				if (!source.isFile()) {
					// throw error because path is wrong
				}

				// TODO: Create the inputStream using new FileInputStream
				InputStream fileInputStream = new FileInputStream(source.toString());
				// TODO: Instantiate the scanner with the input stream and error object
				Scanner myScanner = new Scanner(fileInputStream, _errors);
				// TODO: Instantiate the parser with the scanner and error object
				Parser myParser = new Parser(myScanner, _errors);
				// TODO: Call the parser's parse function
				try {
					ASTIdentifier disp = new ASTIdentifier();
					Package ASTER = myParser.parse();
					// TODO: Check if any errors exist, if so, println("Error")
					//  then output the errors


					if (_errors.hasErrors()) {
						System.out.println("Error");
						_errors.outputErrors();
					}
					else{
						try {
							disp.showTree(ASTER);
							System.out.println("Success");
						}
						catch (Error e){
							_errors.reportError(e.toString());
							System.out.println("Error");
							_errors.outputErrors();
						}
						System.out.println(disp.SId);

					}
				} catch (Throwable e) {
					_errors.reportError("Exception at " + myScanner.getPos().toString());
					if (_errors.hasErrors()) {
						System.out.println("Error");
						_errors.outputErrors();
					}
					// TODO: If there are no errors, println("Success")

				}
			}
		}
		catch (Error | FileNotFoundException e){

		}

		}
	}

