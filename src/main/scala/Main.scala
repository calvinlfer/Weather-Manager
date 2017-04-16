import com.github.calvin.{PersistenceQueryExample, Server}

object Main extends App {
  def usage(): Unit = {
    println("Usage: command/query")
    sys.exit(1)
  }

  val application = args.headOption
  application.fold(usage()) {
    case "command" => new Server
    case "query" => new PersistenceQueryExample
    case _ => usage()
  }
}
