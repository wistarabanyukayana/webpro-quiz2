package id.its.tracker.model;

public record Transaction(long id, String type, long amount, String notes, String date, String time,
                          long categoryId, String category) {
}
