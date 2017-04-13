import ch.qos.logback.core.*;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

appender(name="CONSOLE", clazz=ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "name=weather-manager date=%date{ISO8601} level=%level message=%msg\n"
    }
}

logger(name="com.github.calvin", level=DEBUG)

root(level=INFO, appenderNames=["CONSOLE"])