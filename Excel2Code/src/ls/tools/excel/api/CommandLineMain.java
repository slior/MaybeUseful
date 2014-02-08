package ls.tools.excel.api;


import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.cli.OptionBuilder.hasArg;
import static org.apache.commons.cli.OptionBuilder.withArgName;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import ls.tools.excel.FormulaConverter;
import ls.tools.excel.Function;
import ls.tools.excel.FunctionFormatter;
import ls.tools.excel.serialize.js.JSFormatter;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import fj.data.List;

public final class CommandLineMain
{

	private static final String OUT_FILE = "output";
	private static final String LANGUAGE = "language";
	private static final String NAMES = "names";
	private static final String SOURCE = "source";
	private static final String HELP = "help";
	private static final String NL = System.getProperty("line.separator");
	private final Options options = new Options();
	
	@SuppressWarnings("static-access")
	public CommandLineMain()
	{
		options.addOption(OptionBuilder.withDescription("Usage help information").create(HELP));
		options.addOption(hasArg().withArgName("file").withDescription("The Excel source file name").create(SOURCE));
		options.addOption(hasArg().withArgName("file").withDescription("The output file").create(OUT_FILE));
		options.addOption(withArgName("name1,name2,...").hasArgs().withDescription("The names of the cells containing the formulas to convert").create(NAMES));
		options.addOption(hasArg().withArgName("lang").withDescription("The target language to generate code for").create(LANGUAGE));
	}
	

	private void dispatch(final String[] args)
	{
		try
		{
			final CommandLine cl = parseAndValidate(args);
			if (cl.hasOption(HELP))
				printUsage();
			else
				readConvertAndOutput(cl);
		}
		catch (ParseException e)
		{
			say("Invalid arguments: " + e.getMessage());
			printUsage();
		}
		catch (InvalidFormatException e)
		{
			say("Invalid workbook format: " + e.getMessage());
		}
		catch (FileNotFoundException e)
		{
			say("Can't find file: " + e.getMessage());
		}
		catch (IOException e)
		{
			say("Can't read file: " + e.getMessage());
		}
	}


	/**
	 * Do the entire conversion process, given the arguments, as parsed in the passed {@link CommandLine command line} object.
	 * <br/>
	 * The command line is assumed to be {@link #parseAndValidate(String[]) valid} at this point.
	 * <br/><br/>
	 * The conversion process:
	 * <ol>
	 * <li>Read the file given in the {@link #SOURCE} argument and parse it as an excel workbook (OpenXML). </li>
	 * <li>Resolve the names, either those given as argument ({@link #NAMES}), or all names in the workbook</li>
	 * <li>Convert the names to functions</li>
	 * <li>Format the resulting functions according to the given language formatter ({@link #LANGUAGE})</li>
	 * <li>Output the result to the given file ({@link #OUT_FILE}) or to console, if no output file is given</li>
	 * </ol>
	 * @param cl The parsed command line object
	 * @throws InvalidFormatException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void readConvertAndOutput(final CommandLine cl) throws InvalidFormatException, FileNotFoundException, IOException
	{
		say("Reading workbook...");
		final XSSFWorkbook wb = workbookFor(cl.getOptionValue(SOURCE));
		say("Resolving names...");
		final String[] names = retrieveNames(cl, wb); //if no names are given, will retrieve all
		say("Translating to functions...");
		final FormulaConverter fc = new FormulaConverter();
		List<Function> functions = fc.formulasFromNamedCells(wb, names);
		say("Formatting to target language...");
		final FunctionFormatter formatter = formatterFor(cl.getOptionValue(LANGUAGE));
		final String output = formatter.format(functions, NL + NL);
		say("Outputting result...");
		writeToFile(cl.getOptionValue(OUT_FILE),output);
		say("Done.");
	}


	private void writeToFile(final String outFilename, final String output) throws IOException
	{
		final boolean realFileRequested = (outFilename != null && !outFilename.equals(""));
		if (realFileRequested)
			try (final BufferedWriter bw =  new BufferedWriter(new FileWriter(outFilename))) { //automatically closes the file
				writeOut(output, bw);
			}
		else writeOut(output,new BufferedWriter(new PrintWriter(System.out)));
	}


	private void writeOut(final String output, final BufferedWriter bw) throws IOException
	{
		bw.write(output);
		bw.flush();
	}


	private FunctionFormatter formatterFor(final String lang)
	{
		checkArgument(lang != null,"Language can't be null");
		if (lang.equalsIgnoreCase("js")) 
			return new JSFormatter();
		else throw new IllegalArgumentException("Unrecognized language: " + lang); //TODO: will need to make this dynamic, to enable plugging in more languages
	}


	private String[] retrieveNames(final CommandLine cl, final XSSFWorkbook wb)
	{
		String[] names = cl.getOptionValues(NAMES);
		if (names == null)
		{
			final int nameCount = wb.getNumberOfNames();
			names = new String[nameCount];
			for (int i = 0; i < nameCount; i++)
				names[i] = wb.getNameAt(i).getNameName();
		}
		return names;
	}

	private XSSFWorkbook workbookFor(final String filename) throws InvalidFormatException, FileNotFoundException, IOException
	{
		return (XSSFWorkbook) WorkbookFactory.create(new FileInputStream("test.xlsx"));
	}


	private void printUsage()
	{
		final HelpFormatter hf = new HelpFormatter();
		hf.printHelp(80, "excel2code <options>", "Excel File to Code Converter.\nValid options:", options, "");
	}


	private void say(final String s) { System.out.println(String.valueOf(s)); }


	private CommandLine parseAndValidate(final String[] args) throws ParseException
	{
		if (args.length <= 0) throw new ParseException("No arguments given");
		final CommandLineParser clParser = new BasicParser();
		final CommandLine cl = clParser.parse(options, args);
		if (!cl.hasOption(HELP) && !cl.hasOption(SOURCE))
			throw new ParseException("Either help or source options must be given");
		return cl;
	}


	public static void main(final String[] args)
	{
		new CommandLineMain().dispatch(args);
		
	}
}
