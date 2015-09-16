import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Scanner;


public class ConvertMln {

	public static void main(String[] args) throws FileNotFoundException {
		String mlnFile = "mln/smoker/mln.txt";
		String weightFile = "mln/smoker/smoker-0.log";
		
		Scanner mlnScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(mlnFile))));
		Scanner weightScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(weightFile))));

		while(mlnScanner.hasNextLine()) {
			String mlnLine = mlnScanner.nextLine();
			String weightLine = weightScanner.nextLine().replaceAll("\\s", "");

			String[] clauseArr = mlnLine.split(":");
			String newClause = weightLine + ":" + clauseArr[1];
			System.out.println(newClause);
		}
		
	}
}
