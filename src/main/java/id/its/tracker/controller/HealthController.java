package id.its.tracker.controller;

import id.its.tracker.database.Database;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/up")
public class HealthController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Database.scalar("SELECT 1");
        res.setContentType("text/plain;charset=UTF-8");
        res.getWriter().write("OK");
    }
}
