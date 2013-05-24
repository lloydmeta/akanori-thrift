namespace java trendServer.gen
namespace rb TrendServer.Gen

typedef i64 Timestamp

struct TrendResult {
	1: required string term;
	2: required double termScore;
}

service TrendThriftServer {

    // Simply returns the current time.
	Timestamp time()
	
	// Get list of trends
	list<TrendResult> currentTrends()
}