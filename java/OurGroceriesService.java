@Service
public class OurGroceriesService {

    @Value("${ourgroceries.email}")
    private String configEmail;

    @Value("${ourgroceries.password}")
    private String configPassword;

    @Value("${ourgroceries.apikey}")
    private String configApiKey;

    @Value("${ourgroceries.listId}")
    private String configListId;

    public String addItem(String email, String password, String listId, String item) throws Exception {
        // Clean up the item
        item = item.trim().substring(0, 1).toUpperCase() + item.trim().substring(1).toLowerCase();

        // Establish a connection to OurGroceries by first logging in and getting their login cookie
        String dataString = "emailAddress=" + URLEncoder.encode(email, "UTF-8") + "&action=sign-me-in&password="
                + URLEncoder.encode(password, "UTF-8") + "&staySignedIn=on";
        String cookie = performLogin(dataString);

        // Get the TeamID with the cookie
        String teamId = performGetTeamId(cookie);

        // Add the item to the list
        return performAddItem(cookie, listId, item, teamId);
    }

    private String performLogin(String dataString) throws Exception {
        URL url = new URL("https://www.ourgroceries.com/sign-in");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(dataString.getBytes());
        }
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Error 64: Response code " + responseCode);
        }

        String setCookieHeader = conn.getHeaderField("Set-Cookie");
        Matcher cookieMatcher = Pattern.compile("Set-Cookie: (.*);Path").matcher(setCookieHeader);
        if (cookieMatcher.find()) {
            return cookieMatcher.group(1);
        } else {
            throw new Exception("No auth cookie found in response");
        }
    }

    private String performGetTeamId(String cookie) throws Exception {
        URL url = new URL("https://www.ourgroceries.com/your-lists/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", cookie);
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Error 84: Response code " + responseCode);
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            Matcher teamIdMatcher = Pattern.compile("g_teamId = \"([A-Za-z]+)\";").matcher(response.toString());
            if (teamIdMatcher.find()) {
                return teamIdMatcher.group(1);
            } else {
                throw new Exception("No teamId found");
            }
        }
    }

    private String performAddItem(String cookie, String listId, String item, String teamId) throws Exception {
        String dataString = "{\"command\":\"insertItem\",\"listId\":\"" + listId + "\",\"value\":\"" + item + "\",\"teamId\":\"" + teamId + "\"}";
        URL url = new URL("https://www.ourgroceries.com/your-lists/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "application/json, text/javascript, */*");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Cookie", cookie);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(dataString.getBytes());
        }
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            return "Invalid credentials. Item not added.";
        } else {
            return "Item added.";
        }
    }
}
