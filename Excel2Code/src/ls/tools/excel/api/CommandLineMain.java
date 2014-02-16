package ls.tools.excel.api;


import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.apache.commons.cli.OptionBuilder.hasArg;
import static org.apache.commons.cli.OptionBuilder.withArgName;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

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

import fj.F2;
import fj.data.List;
import fj.data.Option;

public final class CommandLineMain
{

	private static final int HELP_WIDTH = 120;
	private static final String PROGRAM_NAME = "excel2code";
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
		options.addOption(hasArg().withArgName("lang").withDescription("The target language to generate code for. Can also be the class name for the formatter to use.").create(LANGUAGE));
	}
	

	private void dispatch(final String[] args)
	{
		try
		{
			final CommandLine cl = parseAndValidate(args);
			if (cl.hasOption(HELP))
				printUsage();
			else
			{
				say("Processing: " + command(cl));
				readConvertAndOutput(cl);
			}
		}
		catch (ParseException e)
		{
			say("Invalid arguments: " + e.getMessage());
			printUsage();
		}
		catch (InvalidFormatException | IOException e)
		{
			say("Something went wrong reading the source file: " + e.getMessage());
		}
		catch (RuntimeException e)
		{
			say("Error occurred: " + e.getMessage());
		}
	}


	private String command(final CommandLine cl)
	{
		final List<org.apache.commons.cli.Option> options = List.list(cl.getOptions());
		return options.foldLeft(new F2<String,org.apache.commons.cli.Option,String>() {
			@Override public String f(String accum, org.apache.commons.cli.Option opt) {
				return accum + format("-%1$s %2$s ", opt.getOpt(), opt.getValue());
			}
		}, PROGRAM_NAME + " ");
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
		say("Output target: " + (realFileRequested ? outFilename : "Console") + NL);
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
		else 
		{
			final Option<FunctionFormatter> ffOpt = loadFormatter(lang);
			return ffOpt.valueE("Unrecognized language: " + lang);
		}
	}


	private Option<FunctionFormatter> loadFormatter(final String formatterClassName)
	{
		checkArgument(formatterClassName != null,"Can't have a null formatter class name");
		try
		{
			@SuppressWarnings("unchecked")
			Class<FunctionFormatter>  cls = (Class<FunctionFormatter>) Class.forName(formatterClassName);
			return Option.iif(cls != null, cls.newInstance());
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
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
		return (XSSFWorkbook) WorkbookFactory.create(new FileInputStream(filename));
	}


	private void printUsage()
	{
		final HelpFormatter hf = new HelpFormatter();
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		pw.println("Excel File to Code Converter.\n");
		hf.printUsage(pw, HELP_WIDTH, PROGRAM_NAME, options);
		pw.println("Options:");
		hf.printOptions(pw, HELP_WIDTH, options, 2, 0);
		say(sw.toString());
	}

	private CommandLine parseAndValidate(final String[] args) throws ParseException
	{
		if (args.length <= 0) throw new ParseException("No arguments given");
		final CommandLineParser clParser = new BasicParser();
		final CommandLine cl = clParser.parse(options, args);
		if (!cl.hasOption(HELP) && !cl.hasOption(SOURCE))
			throw new ParseException("Either help or source options must be given");
		return cl;
	}

	private void say(final String s) { System.out.println(String.valueOf(s)); }

	public static void main(final String[] args)
	{
		new CommandLineMain().dispatch(args);
		
	}
}
