package i5.las2peer.services.servicePackage;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.ws.rs.*;

import i5.las2peer.api.Service;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.restMapper.tools.ValidationResult;
import i5.las2peer.restMapper.tools.XMLCheck;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.servicePackage.database.DatabaseManager;
import i5.las2peer.services.servicePackage.storage.StorageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * LAS2peer Service
 * <p>
 * This is a template for a very basic LAS2peer service
 * that uses the LAS2peer Web-Connector for RESTful access to it.
 * <p>
 * Note:
 * If you plan on using Swagger you should adapt the information below
 * in the ApiInfo annotation to suit your project.
 * If you do not intend to provide a Swagger documentation of your service API,
 * the entire ApiInfo annotation should be removed.
 */
@Path("/swevaeval")
@Version("0.1") // this annotation is used by the XML mapper
@Api
@SwaggerDefinition(
        info = @Info(
                title = "LAS2peer Template Service",
                version = "0.1",
                description = "A LAS2peer Template Service for demonstration purposes.",
                termsOfService = "http://your-terms-of-service-url.com",
                contact = @Contact(
                        name = "John Doe",
                        url = "provider.com",
                        email = "john.doe@provider.com"
                ),
                license = @License(
                        name = "your software license name",
                        url = "http://your-software-license-url.com"
                )
        ))
public class SwevaEvaluation extends Service {

    // instantiate the logger class
    private final L2pLogger logger = L2pLogger.getInstance(StorageService.class.getName());
    ArrayList<EvalItem> items = new ArrayList<EvalItem>();
    /*
     * Database configuration
     */
    private String jdbcDriverClassName;
    private String jdbcLogin;
    private String jdbcPass;
    private String jdbcUrl;
    private String jdbcSchema;
    private DatabaseManager dbm;

    public SwevaEvaluation() {
        // read and set properties values
        // IF THE SERVICE CLASS NAME IS CHANGED, THE PROPERTIES FILE NAME NEED TO BE CHANGED TOO!
        setFieldValues();
        // instantiate a database manager to handle database connection pooling and credentials
        dbm = new DatabaseManager(jdbcDriverClassName, jdbcLogin, jdbcPass, jdbcUrl, jdbcSchema);
    }

    // //////////////////////////////////////////////////////////////////////////////////////
    // Service methods.
    // //////////////////////////////////////////////////////////////////////////////////////


    @GET
    @Path("/items")
    @Produces(MediaType.APPLICATION_JSON)

    public HttpResponse getAllItems() {
        StringBuilder returnString = new StringBuilder();
        returnString.append("{\"items\":[");


        for (int i = 0; i < items.size(); i++) {
            EvalItem evalItem = items.get(i);
            returnString.append("{");
            returnString.append("\"id\":" + "\"" + evalItem.id + "\",");
            returnString.append("\"ratings\":" + "[");

            for (int j = 0; j < evalItem.ratings.size(); j++) {
                EvalRating rating = evalItem.ratings.get(j);
                returnString.append("{");

                returnString.append("\"name\":" + "\"" + rating.name + "\",");
                returnString.append("\"type\":" + "\"" + rating.type + "\",");
                returnString.append("\"rating\":" + "\"" + Float.toString(rating.rating) + "\",");
                returnString.append("\"review\":" + "\"" + rating.review + "\"");

                returnString.append("}");
                if (j < evalItem.ratings.size() - 1) {
                    returnString.append(",");
                }
            }
            returnString.append("]");
            returnString.append("}");
            if (i < items.size() - 1) {
                returnString.append(",");
            }
        }

        returnString.append("]}");
        return new HttpResponse(returnString.toString(), HttpURLConnection.HTTP_OK);
    }

    @PUT
    @Path("/items/{id}")
    public HttpResponse addItem(@PathParam("id") String id) {

        boolean hasId = false;
        for (int i = 0; i < items.size(); i++) {
            EvalItem evalItem = items.get(i);
            if (evalItem.id.equals(id)) {
                hasId = true;
                break;
            }
        }
        if (hasId) {
            return new HttpResponse("", HttpURLConnection.HTTP_CONFLICT);
        } else {
            EvalItem item = new EvalItem();
            item.id = id;
            items.add(item);
            return new HttpResponse("", HttpURLConnection.HTTP_CREATED);
        }
    }

    @POST
    @Path("/itemsInit")
    public HttpResponse initItems(@ContentParam() String content) {
        JSONObject body = (JSONObject) JSONValue.parse(content);
        JSONArray jsonItems = (JSONArray) body.get("items");
        items.clear();
        for (int i = 0; i < jsonItems.size(); i++) {
            String id = (String) jsonItems.get(i);
            EvalItem item = new EvalItem();
            item.id = id;
            items.add(item);
        }
        return new HttpResponse("", HttpURLConnection.HTTP_CREATED);
    }

    @POST
    @Path("/items")
    public HttpResponse setItems(@ContentParam() String content) {

        JSONObject body = (JSONObject) JSONValue.parse(content);
        JSONArray jsonItems = (JSONArray) body.get("items");

        items.clear();

        for (int i = 0; i < jsonItems.size(); i++) {
            JSONObject jsonItem = (JSONObject) jsonItems.get(i);
            String id = (String) jsonItem.get("id");
            EvalItem evalItem = new EvalItem();
            evalItem.id = id;

            JSONArray jsonRatings = (JSONArray) jsonItem.get("ratings");
            for (int j = 0; j < jsonRatings.size(); j++) {
                JSONObject jsonRating = (JSONObject) jsonRatings.get(j);
                String name = (String) jsonRating.get("name");
                String type = (String) jsonRating.get("type");
                Float rating = Float.parseFloat((String) jsonRating.get("rating"));
                String review = (String) jsonRating.get("review");

                EvalRating evalRating = new EvalRating();
                evalRating.name = name;
                evalRating.type = type;
                evalRating.rating = rating;
                evalRating.review = review;
                evalItem.ratings.add(evalRating);
            }
            items.add(evalItem);
        }
        return new HttpResponse("", HttpURLConnection.HTTP_CREATED);
    }

    @POST
    @Path("/items/{id}")
    public HttpResponse addRating(@PathParam("id") String id, @ContentParam() String content) {
        JSONObject body = (JSONObject) JSONValue.parse(content);
        String name = (String) body.get("name");
        String type = (String) body.get("type");
        Float rating = Float.parseFloat((String) body.get("rating"));
        String review = (String) body.get("review");


        for (int i = 0; i < items.size(); i++) {
            EvalItem item = items.get(i);
            if (item.id.equals(id)) {

                boolean hasRating = false;
                for (int j = 0; j < item.ratings.size(); j++) {
                    EvalRating itemRating = item.ratings.get(j);

                    if (itemRating.name.equals(name)) {
                        hasRating = true;
                        itemRating.type = type;
                        itemRating.rating = rating;
                        itemRating.review = review;
                        break;
                    }
                }

                if (!hasRating) {
                    EvalRating evalRating = new EvalRating();
                    evalRating.name = name;
                    evalRating.type = type;
                    evalRating.rating = rating;
                    evalRating.review = review;
                    item.ratings.add(evalRating);
                }
            }
        }
        return new HttpResponse("", HttpURLConnection.HTTP_CREATED);
    }

    // //////////////////////////////////////////////////////////////////////////////////////
    // Methods required by the LAS2peer framework.
    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Method for debugging purposes.
     * Here the concept of restMapping validation is shown.
     * It is important to check, if all annotations are correct and consistent.
     * Otherwise the service will not be accessible by the WebConnector.
     * Best to do it in the unit tests.
     * To avoid being overlooked/ignored the method is implemented here and not in the test section.
     *
     * @return true, if mapping correct
     */
    public boolean debugMapping() {
        String XML_LOCATION = "./restMapping.xml";
        String xml = getRESTMapping();

        try {
            RESTMapper.writeFile(XML_LOCATION, xml);
        } catch (IOException e) {
            // write error to logfile and console
            logger.log(Level.SEVERE, e.toString(), e);
            // create and publish a monitoring message
            L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
        }

        XMLCheck validator = new XMLCheck();
        ValidationResult result = validator.validate(xml);

        if (result.isValid()) {
            return true;
        }
        return false;
    }

    /**
     * This method is needed for every RESTful application in LAS2peer. There is no need to change!
     *
     * @return the mapping
     */
    public String getRESTMapping() {
        String result = "";
        try {
            result = RESTMapper.getMethodsAsXML(this.getClass());
        } catch (Exception e) {
            // write error to logfile and console
            logger.log(Level.SEVERE, e.toString(), e);
            // create and publish a monitoring message
            L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
        }
        return result;
    }

}
