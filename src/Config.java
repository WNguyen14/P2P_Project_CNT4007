/*
This class is for parsing and holding information in the Common.cfg file
 */

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

public class Config {

	private final int numberOfPreferredNeighbors;
	private final int unchokingInterval;
	private final int optimisticUnchokingInterval;
	private final String configFileName;
	private final int fileSize;
	private final int pieceSize;

	public Config(String fileName) throws FileNotFoundException {
		Scanner in = new Scanner(new FileReader(fileName));
		this.numberOfPreferredNeighbors = Integer.parseInt(in.nextLine().split(" ")[1]);
		this.unchokingInterval = Integer.parseInt(in.nextLine().split(" ")[1]);
		this.optimisticUnchokingInterval = Integer.parseInt(in.nextLine().split(" ")[1]);
		this.configFileName = in.nextLine().split(" ")[1];
		this.fileSize = Integer.parseInt(in.nextLine().split(" ")[1]);
		this.pieceSize = Integer.parseInt(in.nextLine().split(" ")[1]);
		in.close();
	}

	public int getNumberOfPreferredNeighbors() {
		return numberOfPreferredNeighbors;
	}

	public int getUnchokingInterval() {
		return unchokingInterval;
	}

	public int getOptimisticUnchokingInterval() {
		return optimisticUnchokingInterval;
	}

	public String getConfigFileName() {
		return configFileName;
	}

	public int getFileSize() {
		return fileSize;
	}

	public int getPieceSize() {
		return pieceSize;
	}
}
