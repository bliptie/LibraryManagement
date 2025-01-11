/*
 * LibraryModel.java
 * Author:
 * Created on:
 */



import javax.swing.*;
import javax.xml.transform.Result;
import java.sql.*;
import java.sql.Date;
import java.util.ArrayList;
import java.util.*;

import static javax.swing.JOptionPane.showMessageDialog;


public class LibraryModel {

    // For use in creating dialogs and making them modal
    private JFrame dialogParent;
    private Connection con = null;

    // variable to store the most recent searched isbn
    private int lastSearchedISBN = 0;

    public LibraryModel(JFrame parent, String userid, String password) {

	dialogParent = parent;
        try{
            Class.forName("org.postgresql.Driver");
        }
        catch (ClassNotFoundException cnfe){
            System.out.println("Can not find"+
                    "the driver class: "+
                    "\nEither I have not installed it"+
                    "properly or \n postgresql.jar "+
                    " file is not in my CLASSPATH)");
        }
        String url = "jdbc:postgresql:"+ "//db.ecs.vuw.ac.nz/" + userid+ "_jdbc";
        try{
            con = DriverManager.getConnection(url,
                    userid, password);
            con.setAutoCommit(false);
        }
        catch (SQLException sqlex){
            System.out.println("Can not connect");
            System.out.println(sqlex.getMessage());
        }

    }

    // make a check that checks if the book is actually available, and not null
    public String bookLookup(int isbn) {
        // check if book exists
        String bookCheck = "SELECT COUNT(*) AS count FROM book WHERE isbn = ? ";
        try {
            PreparedStatement prep = con.prepareStatement(bookCheck);
            prep.setInt(1, isbn);
            ResultSet rs = prep.executeQuery();
            rs.next();
            if(rs.next() && rs.getInt("count") == 0){
                return "Book not found";
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
        }
        //book lookup
        StringBuilder s = new StringBuilder("Book lookup:\n");

        try {
            PreparedStatement prep = con.prepareStatement("SELECT b.isbn, b.title, b.edition_no, b.numofcop, b.numleft, STRING_AGG(a.surname, ', ') AS authors " +
                    "FROM book AS b NATURAL JOIN book_author AS ba " +
                    "NATURAL JOIN author AS a WHERE b.isbn = ? GROUP BY b.isbn");
            prep.setInt(1, isbn);

            ResultSet rs = prep.executeQuery();
            while(rs.next()){

                s.append(String.format("\t%d: %s\n" +
                        "\tEdition: %d - Number of Copies: %d - Copies left: %d\n" +
                        "\tAuthors: %s", rs.getInt("isbn"), rs.getString("title").strip(), rs.getInt("edition_no"), rs.getInt("numofcop"), rs.getInt("numleft"),
                        rs.getString("authors").strip()));
            }
        }
        catch (SQLException ex){
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
        }
        lastSearchedISBN = isbn;
	    return s.toString();
    }

    public String showCatalogue() {
        StringBuilder s = new StringBuilder();
        try{
            PreparedStatement prep = con.prepareStatement("SELECT b.*, STRING_AGG(a.surname, ', ') AS authors FROM book AS b JOIN book_author AS ba ON b.isbn = ba.isbn JOIN author AS a ON ba.authorid = a.authorid GROUP BY b.isbn");
            ResultSet rs = prep.executeQuery();
            while(rs.next()){
                s.append(String.format("%d: %s\n" +
                        "Edition: %d - Number of Copies: %d - Copies left: %d\n"+
                        "Authors: %s\n", rs.getInt("isbn"), rs.getString("title").strip(), rs.getInt("edition_no"), rs.getInt("numofcop"), rs.getInt("numleft"),
                        rs.getString("authors").strip()));
            }
        }catch (SQLException ex){
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
        }
	    return s.toString();
    }

    public String showLoanedBooks() {
        StringBuilder s = new StringBuilder();
        try {
            PreparedStatement prep = con.prepareStatement("SELECT b.isbn, b.title, b.edition_no, b.numofcop, b.numleft, STRING_AGG(a.surname, ', ') AS authors, c.* FROM book AS b NATURAL JOIN book_author AS ba NATURAL JOIN author AS a NATURAL JOIN customer AS c NATURAL JOIN cust_book AS cb WHERE b.isbn = ? AND b.isbn = cb.isbn GROUP BY b.isbn, c.customerid");
            prep.setInt(1, lastSearchedISBN);

            ResultSet rs = prep.executeQuery();
            rs.next();
            s.append(String.format("%d: %s\n" +
                    "\tEdition: %d - Number of Copies: %d - Copies left: %d\n" +
                    "\tAuthors: %s\n", rs.getInt("isbn"), rs.getString("title").strip(), rs.getInt("edition_no"), rs.getInt("numofcop"), rs.getInt("numleft"), rs.getString("authors").strip()));
            s.append("\tBorrowers:\n");
            while(rs.next()){
                s.append(String.format("\t\t%d: %s, %s - %s\n", rs.getInt("customerid"), rs.getString("f_name").strip(), rs.getString("l_name").strip(), rs.getString("city")));
            }
        }
        catch (SQLException ex){
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
        }
	return s.toString();
    }

    public String showAuthor(int authorID) {
        StringBuilder author = new StringBuilder();
        try{
            PreparedStatement authorPrep = con.prepareStatement("SELECT a.authorid, a.name, a.surname FROM author AS a WHERE a.authorid = ?");
            PreparedStatement bookPrep = con.prepareStatement("SELECT b.isbn, b.title FROM author AS a NATURAL JOIN book_author AS ba NATURAL JOIN book AS b WHERE a.authorid = ? GROUP BY a.authorid, b.isbn");
            authorPrep.setInt(1, authorID);
            bookPrep.setInt(1, authorID);

            ResultSet authorrs = authorPrep.executeQuery();
            ResultSet bookrs = bookPrep.executeQuery();
            while(authorrs.next()){
                author.append(String.format("Show Author:\n" +
                        "\t%d - %s %s\n" +
                        "\tBooks written:\n", authorrs.getInt("authorid"),authorrs.getString("name").strip(), authorrs.getString("surname").strip()));

            }
            while(bookrs.next()){
                author.append(String.format("\t\t%d - %s\n", bookrs.getInt("isbn"), bookrs.getString("title").strip()));
            }
        } catch (SQLException ex){
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
        }

	return author.toString();
    }

    public String showAllAuthors() {
	    StringBuilder authorList = new StringBuilder();
        String s = "SELECT * FROM author ORDER BY authorid";

        try {
            Statement statement = con.createStatement();
            ResultSet rs = statement.executeQuery(s);
            while (rs.next()) {
                authorList.append(String.format("\t%d: %s, %s\n", rs.getInt("authorid"), rs.getString("surname").strip(), rs.getString("name")));
            }
        }
        catch (SQLException ex){
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
        }

        return authorList.toString();
    }

    public String showCustomer(int customerID) {
	    StringBuilder s = new StringBuilder("Show Customer: \n");
        try{
            PreparedStatement prep = con.prepareStatement("""
            SELECT * FROM customer c FULL OUTER JOIN cust_book cb ON c.customerid = cb.customerid 
            FULL OUTER JOIN book b ON cb.isbn = b.isbn WHERE c.customerid = ?""");
            prep.setInt(1, customerID);

            ResultSet rs = prep.executeQuery();
            rs.next();
            s.append(String.format("\t%d: %s, %s - %s\n", rs.getInt("customerid"), rs.getString("f_name").strip(), rs.getString("l_name").strip(), rs.getString("city").strip()));
            s.append("\tBooks borrowed:\n");
            if (rs.getString("title") == null) {
                s.append("\t\tNone\n");
            } else {
                s.append(String.format("\t\t%d: %s\n", rs.getInt("isbn"), rs.getString("title").strip()));
            }
            while(rs.next()){
                s.append(String.format("\t\t%d: %s\n", rs.getInt("isbn"), rs.getString("title").strip()));
            }
        } catch (SQLException ex){
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
        }
        return s.toString();
    }

    public String showAllCustomers() {
        StringBuilder customerList = new StringBuilder();
        String s = "SELECT * FROM customer ORDER BY customerid";

        try {
            Statement statement = con.createStatement();
            ResultSet rs = statement.executeQuery(s);
            while (rs.next()) {
                customerList.append(String.format("\t%d: %s, %s, %s\n", rs.getInt("customerid"), rs.getString("l_name").strip(),
                        rs.getString("f_name").strip(),
                        rs.getString("city") != null ? rs.getString("city") : "(not city)"));

            }
        }
        catch (SQLException ex){
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
        }
        return customerList.toString();
    }

    // includes checks for the book existing , having enough copies, and the customer existing
    public String borrowBook(int isbn, int customerID, int day, int month, int year) {
        try {
            con.beginRequest();
            String bookCheck = "SELECT numleft FROM book WHERE isbn = ? FOR UPDATE"; // added for update - locks row
            try {
                PreparedStatement prep = con.prepareStatement(bookCheck);
                prep.setInt(1, isbn);
                ResultSet rs = prep.executeQuery();
                if (rs.next()) { // book exists check
                    if (rs.getInt("numleft") <= 0) { // enough copies check
                        return "No copies left";
                    }
                } else {
                    return "Book does not exist";
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                System.out.println(ex.getSQLState());
                System.out.println(ex.getErrorCode());
            }

            // customer exists check
            String customerCheck = "SELECT * FROM customer WHERE customerid = ? FOR UPDATE";
            try {
                PreparedStatement prep = con.prepareStatement(customerCheck);
                prep.setInt(1, customerID);
                ResultSet rs = prep.executeQuery();
                if (!rs.next()) {
                    return "Customer does not exist";
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                System.out.println(ex.getSQLState());
                System.out.println(ex.getErrorCode());
            }
//            String s = "SELECT * FROM cust_book";

            // borrowing the book
            String insertString = "INSERT INTO cust_book (isbn, customerid, duedate) VALUES (?, ?, ?)";
            String updateString = "UPDATE book SET numleft = numleft - 1 WHERE isbn = ?"; // need to run this
//            PreparedStatement prep = con.prepareStatement(s);
//            ResultSet res = prep.executeQuery();

            try {
                PreparedStatement prep = con.prepareStatement(insertString);

                prep.setInt(1, isbn);
                prep.setInt(2, customerID);
                prep.setDate(3, new Date(Long.parseLong(year + "" + month + "" + day)));

                // check it was inserted
                int rowsAffected = prep.executeUpdate();
                if (rowsAffected == 0) {
                    return "Error inserting book: " + isbn + " to customer: " + customerID;
                }
                // run update
                prep = con.prepareStatement(updateString);
                prep.setInt(1, isbn);
                // check it was updated
                int rowsAffected2 = prep.executeUpdate();
                if (rowsAffected2 == 0) {
                    return "Error updating book: " + isbn + " to customer: " + customerID;
                }
                showMessageDialog(dialogParent, "Paused while checking if rows are locked");
                // commit transaction
                con.commit();
            } catch (SQLException ex) {
                con.rollback(); // undo all changes when error
                System.out.println(ex.getMessage());
                System.out.println(ex.getSQLState());
                System.out.println(ex.getErrorCode());
                return "Error issuing book: " + isbn + " to customer: " + customerID;
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
            return "Error";
        }

        return "Successfully issued book: " + isbn + " to customer: " + customerID;
    }

    public String returnBook(int isbn, int customerid) {
        // update the copies left in the book table
        // delete the row in cust_book
        try {
            con.beginRequest();
            String bookCheck = "SELECT numleft FROM book WHERE isbn = ? FOR UPDATE"; // added for update - locks row
            try {
                PreparedStatement prep = con.prepareStatement(bookCheck);
                prep.setInt(1, isbn);
                ResultSet rs = prep.executeQuery();
                if (!rs.next()) { // book exists check
                    return "Book does not exist";
                }

            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                System.out.println(ex.getSQLState());
                System.out.println(ex.getErrorCode());
            }

            // customer exists check
            var customerCheck = "SELECT * FROM customer WHERE customerid = ? FOR UPDATE";
            try {
                PreparedStatement prep = con.prepareStatement(customerCheck);
                prep.setInt(1, customerid);
                ResultSet rs = prep.executeQuery();
                if (!rs.next()) {
                    return "Customer does not exist";
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                System.out.println(ex.getSQLState());
                System.out.println(ex.getErrorCode());
            }
            //returning the book
            String deleteString = "DELETE FROM cust_book WHERE isbn = ? AND customerid = ?";
            String updateString = "UPDATE book SET numleft = numleft + 1 WHERE isbn = ?";

            try{
                PreparedStatement prep = con.prepareStatement(deleteString);
                prep.setInt(1, isbn);
                prep.setInt(2, customerid);

                // checking if it was deleted
                int rowsAffected = prep.executeUpdate();
                if (rowsAffected == 0) {
                    return "Error deleting book: " + isbn + " from customer: " + customerid;
                }

                // run update
                prep = con.prepareStatement(updateString);
                prep.setInt(1, isbn);

                // check it was updated
                int rowsAffected2 = prep.executeUpdate();
                if (rowsAffected2 == 0) {
                    return "Error updating book: " + isbn + " to customer: " + customerid;
                }
                // commit transaction
                con.commit();
            } catch (SQLException ex){
                con.rollback(); // undo all changes when error
                System.out.println(ex.getMessage());
                System.out.println(ex.getSQLState());
                System.out.println(ex.getErrorCode());
                return "Error returning book: " + isbn + " to customer: " + customerid;
            }


        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
            return "Error";
        }

        return "Successfully returned book: " + isbn + " to customer: " + customerid;
    }

    public void closeDBConnection() {
        try {
            con.close();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
        }
    }

    public String deleteCus(int customerID) {
        //checks to check:
        //does the customer have a book borrowed
        //if no, delete the customer
        //if yes, throw an error
        try{
            con.beginRequest();
            String bookCheck = "SELECT * FROM cust_book WHERE customerid = ? FOR UPDATE"; // added for update - locks row
            try {
                PreparedStatement prep = con.prepareStatement(bookCheck);
                prep.setInt(1, customerID);
                ResultSet rs = prep.executeQuery();
                if (rs.next()) { // book exists check
                    return "Customer has a book borrowed";
                }

            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                System.out.println(ex.getSQLState());
                System.out.println(ex.getErrorCode());
                return "Error deleting customer: " + customerID;
            }

            String deleteString = "DELETE FROM customer WHERE customerid = ?";
            try{
                PreparedStatement prep = con.prepareStatement(deleteString);
                prep.setInt(1, customerID);
                // checking if it was deleted
                int rowsAffected = prep.executeUpdate();
                if (rowsAffected == 0) {
                    return "Error deleting customer: " + customerID;
                }
                // commit transaction
                con.commit();
            } catch (SQLException ex){
                con.rollback(); // undo all changes when error
                System.out.println(ex.getMessage());
                System.out.println(ex.getSQLState());
                System.out.println(ex.getErrorCode());
                return "Error deleting customer: " + customerID;
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
            return "Error";
        }

    	return "Deleted Customer: " + customerID;
    }

    public String deleteAuthor(int authorID) {
        //checks to check:
        //does the author have a book
        //if no, delete the author
        //if yes, throw an error
        try{
            con.beginRequest();
            String bookCheck = "SELECT * FROM book_author WHERE authorID = ? FOR UPDATE"; // added for update - locks row
            try {
                PreparedStatement prep = con.prepareStatement(bookCheck);
                prep.setInt(1, authorID);
                ResultSet rs = prep.executeQuery();
                if (rs.next()) { // book exists check
                    return "Author has a book";
                }

            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                System.out.println(ex.getSQLState());
                System.out.println(ex.getErrorCode());
            }

            String deleteString = "DELETE FROM author WHERE authorID = ?";
            try{
                PreparedStatement prep = con.prepareStatement(deleteString);
                prep.setInt(1, authorID);
                // checking if it was deleted
                int rowsAffected = prep.executeUpdate();
                if (rowsAffected == 0) {
                    return "Error deleting author: " + authorID;
                }
                // commit transaction
                con.commit();
            } catch (SQLException ex){
                con.rollback(); // undo all changes when error
                System.out.println(ex.getMessage());
                System.out.println(ex.getSQLState());
                System.out.println(ex.getErrorCode());
                return "Error deleting author: " + authorID;
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
            return "Error";
        }
    	return "Deleted Author: " + authorID;
    }

    public String deleteBook(int isbn) {
        //checks to check:
        //does the book have a customer/is borrowed
        //if no, delete the book
        //if yes, throw an error
        try{
            con.beginRequest();
            String bookCheck = "SELECT * FROM cust_book WHERE isbn = ? FOR UPDATE"; // added for update - locks row
            try {
                PreparedStatement prep = con.prepareStatement(bookCheck);
                prep.setInt(1, isbn);
                ResultSet rs = prep.executeQuery();
                if (rs.next()) { // book exists check
                    return "Book is borrowed";
                }
            } catch (SQLException ex) {
                con.rollback(); // undo all changes when error
                System.out.println(ex.getMessage());
                System.out.println(ex.getSQLState());
                System.out.println(ex.getErrorCode());
                return "Error deleting book: " + isbn;
            }

            String deleteString = "DELETE FROM book WHERE isbn = ?";
            try{
                PreparedStatement prep = con.prepareStatement(deleteString);
                prep.setInt(1, isbn);
                // checking if it was deleted
                int rowsAffected = prep.executeUpdate();
                if (rowsAffected == 0) {
                    return "Error deleting book: " + isbn;
                }
                // commit transaction
                con.commit();
            } catch (SQLException ex){
                con.rollback(); // undo all changes when error
                System.out.println(ex.getMessage());
                System.out.println(ex.getSQLState());
                System.out.println(ex.getErrorCode());
                return "Error deleting book: " + isbn;
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            System.out.println(ex.getSQLState());
            System.out.println(ex.getErrorCode());
            return "Error";
        }
    	return "Deleted Book:" + isbn;
    }
}