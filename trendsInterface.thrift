namespace java trendServer.gen
namespace rb TrendServer.Gen

typedef i64 Timestamp

struct TrendResult {
	1: required string term;
	2: required double termScore;
}

service TrendThriftServer {

    // Simply returns the current time.
	Timestamp time(),
	
	// Methods for retrieving Trends
	list<TrendResult> currentTrendsDefault(),
	list<TrendResult> currentTrends(1:double minOccurrence, 2:i32 minLength, 3:i32 maxLength, 4:i32 top),
	
	oneway void analyzeAndStoreMorphemes(1:string stringToParse, 2:bool dropBlacklisted, 3:bool onlyWhitelisted)
}