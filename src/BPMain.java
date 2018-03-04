import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BPMain {
    
    private static final String usage = new StringBuilder()
    .append("Usage: bp_main config0 [config1] tracefile\n\n")
    .append("config0 and config1 are strings like m,n,k.\n")
    .append("\tm   : number of bits in global histor\ny")
    .append("\tn   : number of bits in counter\n")
    .append("\tk   : number of bits in the index to BHT.\n\n")
    .append("-tX\t: use X number of bits to index tournament predictors\n")
    .append("-v\t: verbose mode\n\n")
    .append("For example, 1,2,10 means 1 bit global history, 2-bit counters,\n")
    .append("and 10 bits from branch address. The total number of bits in\n")
    .append("an index is m + k.\n")
    .toString();

    public static void main(String[] args) {
        if (args.length < 2) {
            systemError(usage);
        }
        boolean verbose = false;
        String config0 = "";
        String config1 = "";
        // for tournament predictor, optNBits specifies the number of bits from
        // a memory address used to select a tournament predictor (2^optNBits predictors)
        int optNBits = 10;
        // parse argument options
        for (int i = 0; i < args.length - 1; i++) {
            // exclude the filepath when parsing
            String arg = args[i];
            if (arg.equals("-v")) verbose = true;
            else if (arg.indexOf("-t") == 0) optNBits = Integer.parseInt(arg.substring(2));
            else {
                if (config0.length() == 0) {
                    config0 = arg;
                } else {
                    config1 = arg;
                }
            }
        }
        if (verbose) {
            System.out.println("nbits=" + optNBits + ", config0='" + config0 + "', config1='" + config1 + "'");
        }
        if (optNBits <= 3 || optNBits > 20) {
            systemError("The number of bits for selecting a selector in tournament must be between 4 and 20.");
        }
        int[] config0Parsed = parseConfig(config0);
        int[] config1Parsed = parseConfig(config1);
        BranchPredictor bp = new BranchPredictor();
        
        /*
         * If a second configuration was provided and its BHT index size is greater
         * than zero, use tournament-style predictor. Else use local or global predictor.
         */
        if (config1Parsed[2] > 0) {
            bp.configureTournament(optNBits,
                    config0Parsed[0], config0Parsed[1], config0Parsed[2],
                    config1Parsed[0], config1Parsed[1], config1Parsed[2]);
        } else {
            bp.configure(config0Parsed[0], config0Parsed[1], config0Parsed[2]);
        }
        bp.verbose = verbose;
        readFile(bp, args[args.length - 1]);
        bp.report();
    }
    
    private static void readFile(BranchPredictor bp, String filepath) {
        String line;
        BufferedReader br;
        /*
         * This regex pattern matches when the line begins with an address that can
         * (optionally) be prefixed with '0x', followed by one or more alphanumeric characters,
         * followed by whitespace and then either a 1, T, t (taken) or 0, NT, nt (not taken)
         */
        String pattern = "^((0x)*([0-9A-Fa-f]+))\\s+(0|1|T|t|NT|nt)\\s*$";
        Pattern r = Pattern.compile(pattern);
        long addr;
        int outcome;
       
        try {
            br = new BufferedReader(new FileReader(filepath));    
            while ((line = br.readLine()) != null) {
                   Matcher m = r.matcher(line);
                   if (m.find()) {
                       // in the pattern, each set of parentheses represents a "group"
                       addr = Long.decode(m.group(1));
                       outcome = "1Tt".indexOf(m.group(4).charAt(0)) != -1 ? 1 : 0;
                       bp.predict(addr, outcome);
                   }
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    private static void systemError(String s) {
        System.err.println(s);
        System.exit(1);
    }
    
    private static int[] parseConfig(String s) {
        if (s.length() == 0) return new int[] { 0, 0, 0 };
        String[] fields = s.split(",");
        if (fields.length != 3) systemError("Invalid configuration. Need three numbers, like 2,1,10.");
        try {
            int m = Integer.parseInt(fields[0]);
            int n = Integer.parseInt(fields[1]);
            int k = Integer.parseInt(fields[2]);
            if (m < 0 || m > 16 || n < 1 || n > 4 || k < 0 || k > 16) {
                systemError("Arguments do not fall within the correct range.");
            }
            return new int[] { m, n, k };
        } catch (NumberFormatException e) {
            systemError("Invalid arguments (must be numbers).");
            return null;
        }
    }

}
