package huff.http.json

// 10 feb 2017 - 
// The keyword: `message` cannot be used
// here as its reserved in ELK.
//
case class HuffLog(
  service_name : String,
  category: String,
  event_type: String, 
  mesg: String
) 

