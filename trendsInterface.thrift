namespace java trendServer.gen
namespace rb TrendServer.Gen

typedef i64 Timestamp

service TrendThriftServer {

    // Simply returns the current time.
	Timestamp time()
}