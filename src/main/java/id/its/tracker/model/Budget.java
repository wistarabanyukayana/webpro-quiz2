package id.its.tracker.model;

public record Budget(long id, long categoryId, String category, String month, long amount, long spent) {

    public long percent() {
        return (long) (spent * 100.0 / amount);
    }

    public long remaining() {
        return amount - spent;
    }
}
