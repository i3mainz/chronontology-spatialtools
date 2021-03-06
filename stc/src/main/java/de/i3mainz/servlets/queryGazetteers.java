package de.i3mainz.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.i3mainz.classes.BoundingBox;
import de.i3mainz.classes.GazetteerData;
import de.i3mainz.database.PostGIS;
import de.i3mainz.functions.gazetteer.GazetteerDAI;
import de.i3mainz.functions.gazetteer.GazetteerGeonames;
import de.i3mainz.functions.gazetteer.GazetteerGettyTGN;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jdom.JDOMException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class queryGazetteers extends HttpServlet {

	protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		try {
			// PARSE REQUEST
			String upperleft = "50.082665;8.161050";
			String lowerleft = "50.082665;8.371850";
			String upperright = "49.903887;8.161050";
			String lowerright = "49.903887;8.371850";
			if (request.getParameter("upperleft") != null) {
				upperleft = request.getParameter("upperleft");
			}
			if (request.getParameter("lowerleft") != null) {
				lowerleft = request.getParameter("lowerleft");
			}
			if (request.getParameter("upperright") != null) {
				upperright = request.getParameter("upperright");
			}
			if (request.getParameter("lowerright") != null) {
				lowerright = request.getParameter("lowerright");
			}
			String[] upperleftSplit = upperleft.split(";"); //[0]=LAT [1]=LON
			String[] lowerleftSplit = lowerleft.split(";"); //[0]=LAT [1]=LON
			String[] upperrightSplit = upperright.split(";"); //[0]=LAT [1]=LON
			String[] lowerrightSplit = lowerright.split(";"); //[0]=LAT [1]=LON
			// GET BOUNDINGBOX
			BoundingBox bb = new BoundingBox(Double.parseDouble(upperleftSplit[0]),Double.parseDouble(upperleftSplit[1]),Double.parseDouble(lowerleftSplit[0]),Double.parseDouble(lowerleftSplit[1]),Double.parseDouble(upperrightSplit[0]),Double.parseDouble(upperrightSplit[1]),Double.parseDouble(lowerrightSplit[0]),Double.parseDouble(lowerrightSplit[1]));
			// HARVEST DATA
			/*StringBuffer resultTGN = getResultsFromGettyTGN(lowerleftSplit[0], lowerleftSplit[1], upperrightSplit[0], upperrightSplit[1]);
			List<GazetteerData> tgn = ParseTGNJSON(resultTGN);
			System.out.println("TGN size: " + tgn.size());
			StringBuffer resultGEONAMES = getResultsFromGeonames(upperrightSplit[0], upperleftSplit[0], upperrightSplit[1], lowerleftSplit[1]);
			List<GazetteerData> geonames = ParseGEONAMESJSON(resultGEONAMES);
			System.out.println("GEONAMES size: " + geonames.size());
			StringBuffer resultDAI = getResultsFromDaiGazetteer(upperleftSplit[0], upperleftSplit[1], upperrightSplit[0], upperrightSplit[1], lowerrightSplit[0], lowerrightSplit[1], lowerleftSplit[0], lowerleftSplit[1]);
			List<GazetteerData> daigazetteer = ParseDAIJSON(resultDAI);
			System.out.println("DAI GAZETTEER size: " + daigazetteer.size());*/
			StringBuffer resultTGN = GazetteerGettyTGN.getResultsFromGettyTGN(String.valueOf(bb.getLowerleft_lat()), String.valueOf(bb.getLowerleft_lon()), String.valueOf(bb.getUpperright_lat()), String.valueOf(bb.getUpperright_lon()));
			List<GazetteerData> tgn = GazetteerGettyTGN.ParseTGNJSON(resultTGN);
			System.out.println("TGN size: " + tgn.size());
			StringBuffer resultGEONAMES = GazetteerGeonames.getResultsFromGeonames(String.valueOf(bb.getUpperright_lat()), String.valueOf(bb.getLowerleft_lat()), String.valueOf(bb.getUpperright_lon()), String.valueOf(bb.getLowerleft_lon()));
			List<GazetteerData> geonames = GazetteerGeonames.ParseGEONAMESJSON(resultGEONAMES);
			System.out.println("GEONAMES size: " + geonames.size());
			StringBuffer resultDAI = GazetteerDAI.getResultsFromDaiGazetteer(String.valueOf(bb.getUpperleft_lat()), String.valueOf(bb.getUpperleft_lon()), String.valueOf(bb.getUpperright_lat()), String.valueOf(bb.getUpperright_lon()), String.valueOf(bb.getLowerright_lat()), String.valueOf(bb.getLowerright_lon()), String.valueOf(bb.getLowerleft_lat()), String.valueOf(bb.getLowerleft_lon()));
			List<GazetteerData> daigazetteer = GazetteerDAI.ParseDAIJSON(resultDAI);
			System.out.println("DAI GAZETTEER size: " + daigazetteer.size());
			// CREATE GEOJSON
			StringBuffer geojson = new StringBuffer();
			List<String> data = new ArrayList<String>();
			geojson.append("{ \"type\": \"FeatureCollection\", ");
			geojson.append("\"elements\": { \"gettyTGN\": " + tgn.size() + ", \"geonames\": " + geonames.size() + ", \"daiGazetteer\": " + daigazetteer.size() + " },");
			geojson.append("\"features\": [ ");
			// SET BOUNDING BOX
			String bboxfeature = "{ \"type\": \"Feature\", \"geometry\": " + bb.getGeoJSON() + ", \"properties\": {\"type\": \"boundingbox\"} },";
			data.add(bboxfeature);
			// SET GETTY TGN
			for (GazetteerData element : tgn) {
				String lat = element.getLAT().replace("-.", "-0.");
				String lon = element.getLON().replace("-.", "-0.");
				String feature = "{ \"type\": \"Feature\", \"geometry\": {\"type\": \"Point\", \"coordinates\": [" + lon + ", " + lat + "]} , \"properties\": {\"uri\": \"" + element.getURI() + "\", \"name\": \"" + element.getNAME() + "\", \"provenance\": \"" + element.getPROVENANCE() + "\"} },";
				data.add(feature);
			}
			// SET GEONAMES
			for (GazetteerData element : geonames) {
				String lat = element.getLAT().replace("-.", "-0.");
				String lon = element.getLON().replace("-.", "-0.");
				String feature = "{ \"type\": \"Feature\", \"geometry\": {\"type\": \"Point\", \"coordinates\": [" + lon + ", " + lat + "]} , \"properties\": {\"uri\": \"" + element.getURI() + "\", \"name\": \"" + element.getNAME() + "\", \"provenance\": \"" + element.getPROVENANCE() + "\"} },";
				data.add(feature);
			}
			// SET DAI GAZETTEER
			for (GazetteerData element : daigazetteer) {
				String lat = element.getLAT().replace("-.", "-0.");
				String lon = element.getLON().replace("-.", "-0.");
				String feature = "{ \"type\": \"Feature\", \"geometry\": {\"type\": \"Point\", \"coordinates\": [" + lon + ", " + lat + "]} , \"properties\": {\"uri\": \"" + element.getURI() + "\", \"name\": \"" + element.getNAME() + "\", \"provenance\": \"" + element.getPROVENANCE() + "\"} },";
				data.add(feature);
			}
			String lastline = data.get(data.size() - 1).substring(0, data.get(data.size() - 1).length() - 1);
			data.set(data.size() - 1, lastline);
			for (String dataelement : data) {
				geojson.append(dataelement);
			}
			geojson.append("] }");
			// pretty print JSON output (Gson)
			JsonParser parser = new JsonParser();
			JsonObject json = parser.parse(geojson.toString()).getAsJsonObject();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			out.print(gson.toJson(json));
			// set outputformat
			response.setContentType("application/json;charset=UTF-8");
			int z = 0;
		} catch (Exception e) {
			for (StackTraceElement element : e.getStackTrace()) {
				String message = "element: \"" + element.getClassName() + " / " + element.getMethodName() + "() / " + element.getLineNumber() + "\"";
				System.out.println(message);
				out.println(message);
			}
			response.setContentType("text/plain;charset=UTF-8");
		} finally {
			out.close();
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setCharacterEncoding("UTF-8");
		}
	}

	// <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
	/**
	 * Handles the HTTP <code>GET</code> method.
	 *
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Handles the HTTP <code>POST</code> method.
	 *
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Returns a short description of the servlet.
	 *
	 * @return a String containing servlet description
	 */
	@Override
	public String getServletInfo() {
		return "Short description";
	}// </editor-fold>

}
