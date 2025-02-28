import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class OurGroceriesPythonConversionToJava {
    private static final Logger _LOGGER = Logger.getLogger(OurGroceries.class.getName());

    // URLs used
    private static final String BASE_URL = "https://www.ourgroceries.com";
    private static final String SIGN_IN = BASE_URL + "/sign-in";
    private static final String YOUR_LISTS = BASE_URL + "/your-lists/";

    // Cookies
    private static final String COOKIE_KEY_SESSION = "ourgroceries-auth";

    // Form fields when logging in
    private static final String FORM_KEY_USERNAME = "emailAddress";
    private static final String FORM_KEY_PASSWORD = "password";
    private static final String FORM_KEY_ACTION = "action";
    private static final String FORM_VALUE_ACTION = "sign-in";

    // Actions to perform on post API
    private static final String ACTION_GET_LIST = "getList";
    private static final String ACTION_GET_LISTS = "getOverview";
    private static final String ACTION_ITEM_CROSSED_OFF = "setItemCrossedOff";
    private static final String ACTION_ITEM_ADD = "insertItem";
    private static final String ACTION_ITEM_ADD_ITEMS = "insertItems";
    private static final String ACTION_ITEM_REMOVE = "deleteItem";
    private static final String ACTION_ITEM_RENAME = "changeItemValue";
    private static final String ACTION_LIST_CREATE = "createList";
    private static final String ACTION_LIST_REMOVE = "deleteList";
    private static final String ACTION_LIST_RENAME = "renameList";
    private static final String ACTION_GET_MASTER_LIST = "getMasterList";
    private static final String ACTION_GET_CATEGORY_LIST = "getCategoryList";
    private static final String ACTION_ITEM_CHANGE_VALUE = "changeItemValue";
    private static final String ACTION_LIST_DELETE_ALL_CROSSED_OFF = "deleteAllCrossedOffItems";

    // Regex patterns
    private static final Pattern REGEX_TEAM_ID = Pattern.compile("g_teamId = \"(.*)\";");
    private static final Pattern REGEX_STATIC_METALIST = Pattern.compile("g_staticMetalist = (\\[.*\\]);");
    private static final Pattern REGEX_MASTER_LIST_ID = Pattern.compile("g_masterListUrl = \"/your-lists/list/(\\S*)\"");

    // Post body attributes
    private static final String ATTR_LIST_ID = "listId";
    private static final String ATTR_LIST_NAME = "name";
    private static final String ATTR_LIST_TYPE = "listType";
    private static final String ATTR_ITEM_ID = "itemId";
    private static final String ATTR_ITEM_CROSSED = "crossedOff";
    private static final String ATTR_ITEM_VALUE = "value";
    private static final String ATTR_ITEM_CATEGORY = "categoryId";
    private static final String ATTR_ITEM_NOTE = "note";
    private static final String ATTR_ITEMS = "items";
    private static final String ATTR_COMMAND = "command";
    private static final String ATTR_TEAM_ID = "teamId";
    private static final String ATTR_CATEGORY_ID = "categoryId";
    private static final String ATTR_ITEM_NEW_VALUE = "newValue";

    // Properties of returned data
    private static final String PROP_LIST = "list";
    private static final String PROP_ITEMS = "items";

    private String username;
    private String password;
    private String sessionKey;
    private String teamId;
    private String masterListId;
    private String categoryId;

    public OurGroceries(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void login() throws Exception {
        getSessionCookie();
        getTeamId();
        getMasterListId();
        _LOGGER.info("ourgroceries logged in");
    }

    private void getSessionCookie() throws Exception {
        _LOGGER.info("ourgroceries _get_session_cookie");

        HttpClient client = HttpClient.newHttpClient();
        String formData = String.format("%s=%s&%s=%s&%s=%s",
                FORM_KEY_USERNAME, username,
                FORM_KEY_PASSWORD, password,
                FORM_KEY_ACTION, FORM_VALUE_ACTION);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SIGN_IN))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        Map<String, List<String>> headers = response.headers().map();

        List<String> cookies = headers.get("set-cookie");
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.startsWith(COOKIE_KEY_SESSION)) {
                    sessionKey = cookie.split("=")[1].split(";")[0];
                    _LOGGER.info("ourgroceries found _session_key " + sessionKey);
                    return;
                }
            }
        }

        _LOGGER.severe("ourgroceries Could not find cookie session");
        throw new InvalidLoginException("Could not find session cookie");
    }

    private void getTeamId() throws Exception {
        _LOGGER.info("ourgroceries _get_team_id");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(YOUR_LISTS))
                .header("Cookie", COOKIE_KEY_SESSION + "=" + sessionKey)
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        String responseText = response.body();

        Matcher matcher = REGEX_TEAM_ID.matcher(responseText);
        if (matcher.find()) {
            teamId = matcher.group(1);
            _LOGGER.info("ourgroceries found team_id " + teamId);
        }

        matcher = REGEX_STATIC_METALIST.matcher(responseText);
        if (matcher.find()) {
            JSONArray staticMetalist = new JSONArray(matcher.group(1));
            for (int i = 0; i < staticMetalist.length(); i++) {
                JSONObject list = staticMetalist.getJSONObject(i);
                if ("CATEGORY".equals(list.getString("listType"))) {
                    categoryId = list.getString("id");
                    _LOGGER.info("ourgroceries found category_id " + categoryId);
                    break;
                }
            }
        }
    }

    private void getMasterListId() throws Exception {
        _LOGGER.info("ourgroceries _get_master_list_id");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(YOUR_LISTS))
                .header("Cookie", COOKIE_KEY_SESSION + "=" + sessionKey)
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        String responseText = response.body();

        Matcher matcher = REGEX_MASTER_LIST_ID.matcher(responseText);
        if (matcher.find()) {
            masterListId = matcher.group(1);
            _LOGGER.info("ourgroceries found master_list_id " + masterListId);
        }
    }

    public JSONObject getMyLists() throws Exception {
        _LOGGER.info("ourgroceries get_my_lists");
        return post(ACTION_GET_LISTS, null);
    }

    public JSONObject getCategoryItems() throws Exception {
        _LOGGER.info("ourgroceries get_category_items");
        JSONObject otherPayload = new JSONObject();
        otherPayload.put(ATTR_LIST_ID, categoryId);
        return post(ACTION_GET_LIST, otherPayload);
    }

    public JSONObject getListItems(String listId) throws Exception {
        _LOGGER.info("ourgroceries get_list_items");
        JSONObject otherPayload = new JSONObject();
        otherPayload.put(ATTR_LIST_ID, listId);
        JSONObject data = post(ACTION_GET_LIST, otherPayload);
        JSONArray items = data.getJSONObject(PROP_LIST).getJSONArray(PROP_ITEMS);
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            addCrossedOffProp(item);
        }
        return data;
    }

    public JSONObject createList(String name, String listType) throws Exception {
        _LOGGER.info("ourgroceries create_list");
        JSONObject otherPayload = new JSONObject();
        otherPayload.put(ATTR_LIST_NAME, name);
        otherPayload.put(ATTR_LIST_TYPE, listType.toUpperCase());
        return post(ACTION_LIST_CREATE, otherPayload);
    }

    public JSONObject createCategory(String name) throws Exception {
        _LOGGER.info("ourgroceries create_category");
        JSONObject otherPayload = new JSONObject();
        otherPayload.put(ATTR_ITEM_VALUE, name);
        otherPayload.put(ATTR_LIST_ID, categoryId);
        return post(ACTION_ITEM_ADD, otherPayload);
    }

    public JSONObject toggleItemCrossedOff(String listId, String itemId, boolean crossOff) throws Exception {
        _LOGGER.info("ourgroceries toggle_item_crossed_off");
        JSONObject otherPayload = new JSONObject();
        otherPayload.put(ATTR_LIST_ID, listId);
        otherPayload.put(ATTR_ITEM_ID, itemId);
        otherPayload.put(ATTR_ITEM_CROSSED, crossOff);
        return post(ACTION_ITEM_CROSSED_OFF, otherPayload);
    }

    public JSONObject addItemToList(String listId, String value, String category, boolean autoCategory, String note) throws Exception {
        _LOGGER.info("ourgroceries add_item_to_list");
        JSONObject otherPayload = new JSONObject();
        otherPayload.put(ATTR_LIST_ID, listId);
        otherPayload.put(ATTR_ITEM_VALUE, value);
        otherPayload.put(ATTR_ITEM_CATEGORY, category);
        otherPayload.put(ATTR_ITEM_NOTE, note);
        if (autoCategory) {
            otherPayload.remove(ATTR_ITEM_CATEGORY);
        }
        return post(ACTION_ITEM_ADD, otherPayload);
    }

    public JSONObject addItemsToList(String listId, List<Object> items) throws Exception {
        _LOGGER.info("ourgroceries add_items_to_list");
        JSONObject otherPayload = new JSONObject();
        JSONArray itemsArray = new JSONArray();
        for (Object item : items) {
            itemsArray.put(listItemToPayload(item, listId));
        }
        otherPayload.put(ATTR_ITEMS, itemsArray);
        return post(ACTION_ITEM_ADD_ITEMS, otherPayload);
    }

    public JSONObject removeItemFromList(String listId, String itemId) throws Exception {
        _LOGGER.info("ourgroceries remove_item_from_list");
        JSONObject otherPayload = new JSONObject();
        otherPayload.put(ATTR_LIST_ID, listId);
        otherPayload.put(ATTR_ITEM_ID, itemId);
        return post(ACTION_ITEM_REMOVE, otherPayload);
    }

    public JSONObject getMasterList() throws Exception {
        _LOGGER.info("ourgroceries get_master_list");
        JSONObject otherPayload = new JSONObject();
        otherPayload.put(ATTR_LIST_ID, masterListId);
        return post(ACTION_GET_LIST, otherPayload);
    }

    public JSONObject getCategoryList() throws Exception {
        _LOGGER.info("ourgroceries get_category_list");
        JSONObject otherPayload = new JSONObject();
        otherPayload.put(ATTR_TEAM_ID, teamId);
        return post(ACTION_GET_CATEGORY_LIST, otherPayload);
    }

    public JSONObject deleteList(String listId) throws Exception {
        _LOGGER.info("ourgroceries delete_list");
        JSONObject otherPayload = new JSONObject();
        otherPayload.put(ATTR_LIST_ID, listId);
        otherPayload.put(ATTR_TEAM_ID, teamId);
        return post(ACTION_LIST_REMOVE, otherPayload);
    }

    public JSONObject deleteAllCrossedOffFromList(String listId) throws Exception {
        _LOGGER.info("ourgroceries delete_all_crossed_off_from_list");
        JSONObject otherPayload = new JSONObject();
        otherPayload.put(ATTR_LIST_ID, listId);
        return post(ACTION_LIST_DELETE_ALL_CROSSED_OFF, otherPayload);
    }

    public JSONObject addItemToMasterList(String value, String categoryId) throws Exception {
        _LOGGER.info("ourgroceries add_item_to_master_list");
        JSONObject otherPayload = new JSONObject();
        otherPayload.put(ATTR_LIST_ID, masterListId);
        otherPayload.put(ATTR_ITEM_VALUE, value);
        otherPayload.put(ATTR_CATEGORY_ID, categoryId);
        return post(ACTION_ITEM_ADD, otherPayload);
    }

    public JSONObject changeItemOnList(String listId, String itemId, String categoryId, String value) throws Exception {
        _LOGGER.info("ourgroceries change_item_on_list");
        JSONObject otherPayload = new JSONObject();
        otherPayload.put(ATTR_ITEM_ID, itemId);
        otherPayload.put(ATTR_LIST_ID, listId);
        otherPayload.put(ATTR_ITEM_NEW_VALUE, value);
        otherPayload.put(ATTR_CATEGORY_ID, categoryId);
        otherPayload.put(ATTR_TEAM_ID, teamId);
        return post(ACTION_ITEM_CHANGE_VALUE, otherPayload);
    }

    private JSONObject post(String command, JSONObject otherPayload) throws Exception {
        if (sessionKey == null) {
            login();
        }

        HttpClient client = HttpClient.newHttpClient();
        JSONObject payload = new JSONObject();
        payload.put(ATTR_COMMAND, command);

        if (teamId != null) {
            payload.put(ATTR_TEAM_ID, teamId);
        }

        if (otherPayload != null) {
            for (String key : otherPayload.keySet()) {
                payload.put(key, otherPayload.get(key));
            }
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(YOUR_LISTS))
                .header("Cookie", COOKIE_KEY_SESSION + "=" + sessionKey)
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        return new JSONObject(response.body());
    }

    private void addCrossedOffProp(JSONObject item) {
        item.put(ATTR_ITEM_CROSSED, item.optBoolean(ATTR_ITEM_CROSSED, false));
    }

    private JSONObject listItemToPayload(Object item, String listId) {
        JSONObject payload = new JSONObject();
        if (item instanceof String) {
            payload.put(ATTR_ITEM_VALUE, item);
        } else if (item instanceof List) {
            List<?> itemList = (List<?>) item;
            payload.put(ATTR_ITEM_VALUE, itemList.get(0));
            payload.put(ATTR_ITEM_CATEGORY, itemList.size() > 1 ? itemList.get(1) : null);
            payload.put(ATTR_ITEM_NOTE, itemList.size() > 2 ? itemList.get(2) : null);
        }
        payload.put(ATTR_LIST_ID, listId);
        return payload;
    }

    public static class InvalidLoginException extends Exception {
        public InvalidLoginException(String message) {
            super(message);
        }
    }
}
