package Exceptions;

public class PageDoesNotExistException extends Exception {

    public PageDoesNotExistException(String msg) {
        super(msg);
    }
}
