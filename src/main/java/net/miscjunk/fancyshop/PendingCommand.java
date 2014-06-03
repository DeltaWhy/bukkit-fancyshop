package net.miscjunk.fancyshop;

public class PendingCommand {
    enum Type { CREATE, REMOVE, SETADMIN, RENAME };

    private Type type;
    private String[] args;

    public PendingCommand(Type type) {
        this(type, new String[]{});
    }

    public PendingCommand(Type type, String... args) {
        this.type = type;
        this.args = args;
    }

    public Type getType() {
        return type;
    }

    public String[] getArgs() {
        return args;
    }
}
