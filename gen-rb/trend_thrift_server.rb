#
# Autogenerated by Thrift Compiler (0.9.0)
#
# DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
#

require 'thrift'
require 'trends_interface_types'

module TrendServer
  module Gen
    module TrendThriftServer
      class Client
        include ::Thrift::Client

        def time()
          send_time()
          return recv_time()
        end

        def send_time()
          send_message('time', Time_args)
        end

        def recv_time()
          result = receive_message(Time_result)
          return result.success unless result.success.nil?
          raise ::Thrift::ApplicationException.new(::Thrift::ApplicationException::MISSING_RESULT, 'time failed: unknown result')
        end

        def currentTrendsDefault()
          send_currentTrendsDefault()
          return recv_currentTrendsDefault()
        end

        def send_currentTrendsDefault()
          send_message('currentTrendsDefault', CurrentTrendsDefault_args)
        end

        def recv_currentTrendsDefault()
          result = receive_message(CurrentTrendsDefault_result)
          return result.success unless result.success.nil?
          raise ::Thrift::ApplicationException.new(::Thrift::ApplicationException::MISSING_RESULT, 'currentTrendsDefault failed: unknown result')
        end

        def currentTrends(spanInSeconds, minOccurrence, minLength, maxLength, top, dropBlacklisted, onlyWhitelisted)
          send_currentTrends(spanInSeconds, minOccurrence, minLength, maxLength, top, dropBlacklisted, onlyWhitelisted)
          return recv_currentTrends()
        end

        def send_currentTrends(spanInSeconds, minOccurrence, minLength, maxLength, top, dropBlacklisted, onlyWhitelisted)
          send_message('currentTrends', CurrentTrends_args, :spanInSeconds => spanInSeconds, :minOccurrence => minOccurrence, :minLength => minLength, :maxLength => maxLength, :top => top, :dropBlacklisted => dropBlacklisted, :onlyWhitelisted => onlyWhitelisted)
        end

        def recv_currentTrends()
          result = receive_message(CurrentTrends_result)
          return result.success unless result.success.nil?
          raise ::Thrift::ApplicationException.new(::Thrift::ApplicationException::MISSING_RESULT, 'currentTrends failed: unknown result')
        end

        def trendsEndingAt(unixEndAtTime, spanInSeconds, minOccurrence, minLength, maxLength, top, dropBlacklisted, onlyWhitelisted)
          send_trendsEndingAt(unixEndAtTime, spanInSeconds, minOccurrence, minLength, maxLength, top, dropBlacklisted, onlyWhitelisted)
          return recv_trendsEndingAt()
        end

        def send_trendsEndingAt(unixEndAtTime, spanInSeconds, minOccurrence, minLength, maxLength, top, dropBlacklisted, onlyWhitelisted)
          send_message('trendsEndingAt', TrendsEndingAt_args, :unixEndAtTime => unixEndAtTime, :spanInSeconds => spanInSeconds, :minOccurrence => minOccurrence, :minLength => minLength, :maxLength => maxLength, :top => top, :dropBlacklisted => dropBlacklisted, :onlyWhitelisted => onlyWhitelisted)
        end

        def recv_trendsEndingAt()
          result = receive_message(TrendsEndingAt_result)
          return result.success unless result.success.nil?
          raise ::Thrift::ApplicationException.new(::Thrift::ApplicationException::MISSING_RESULT, 'trendsEndingAt failed: unknown result')
        end

        def stringToWords(stringToAnalyze)
          send_stringToWords(stringToAnalyze)
          return recv_stringToWords()
        end

        def send_stringToWords(stringToAnalyze)
          send_message('stringToWords', StringToWords_args, :stringToAnalyze => stringToAnalyze)
        end

        def recv_stringToWords()
          result = receive_message(StringToWords_result)
          return result.success unless result.success.nil?
          raise ::Thrift::ApplicationException.new(::Thrift::ApplicationException::MISSING_RESULT, 'stringToWords failed: unknown result')
        end

        def storeString(stringToStore, unixCreatedAtTime, weeksAgoDataToExpire)
          send_storeString(stringToStore, unixCreatedAtTime, weeksAgoDataToExpire)
        end

        def send_storeString(stringToStore, unixCreatedAtTime, weeksAgoDataToExpire)
          send_message('storeString', StoreString_args, :stringToStore => stringToStore, :unixCreatedAtTime => unixCreatedAtTime, :weeksAgoDataToExpire => weeksAgoDataToExpire)
        end
      end

      class Processor
        include ::Thrift::Processor

        def process_time(seqid, iprot, oprot)
          args = read_args(iprot, Time_args)
          result = Time_result.new()
          result.success = @handler.time()
          write_result(result, oprot, 'time', seqid)
        end

        def process_currentTrendsDefault(seqid, iprot, oprot)
          args = read_args(iprot, CurrentTrendsDefault_args)
          result = CurrentTrendsDefault_result.new()
          result.success = @handler.currentTrendsDefault()
          write_result(result, oprot, 'currentTrendsDefault', seqid)
        end

        def process_currentTrends(seqid, iprot, oprot)
          args = read_args(iprot, CurrentTrends_args)
          result = CurrentTrends_result.new()
          result.success = @handler.currentTrends(args.spanInSeconds, args.minOccurrence, args.minLength, args.maxLength, args.top, args.dropBlacklisted, args.onlyWhitelisted)
          write_result(result, oprot, 'currentTrends', seqid)
        end

        def process_trendsEndingAt(seqid, iprot, oprot)
          args = read_args(iprot, TrendsEndingAt_args)
          result = TrendsEndingAt_result.new()
          result.success = @handler.trendsEndingAt(args.unixEndAtTime, args.spanInSeconds, args.minOccurrence, args.minLength, args.maxLength, args.top, args.dropBlacklisted, args.onlyWhitelisted)
          write_result(result, oprot, 'trendsEndingAt', seqid)
        end

        def process_stringToWords(seqid, iprot, oprot)
          args = read_args(iprot, StringToWords_args)
          result = StringToWords_result.new()
          result.success = @handler.stringToWords(args.stringToAnalyze)
          write_result(result, oprot, 'stringToWords', seqid)
        end

        def process_storeString(seqid, iprot, oprot)
          args = read_args(iprot, StoreString_args)
          @handler.storeString(args.stringToStore, args.unixCreatedAtTime, args.weeksAgoDataToExpire)
          return
        end

      end

      # HELPER FUNCTIONS AND STRUCTURES

      class Time_args
        include ::Thrift::Struct, ::Thrift::Struct_Union

        FIELDS = {

        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class Time_result
        include ::Thrift::Struct, ::Thrift::Struct_Union
        SUCCESS = 0

        FIELDS = {
          SUCCESS => {:type => ::Thrift::Types::I64, :name => 'success'}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class CurrentTrendsDefault_args
        include ::Thrift::Struct, ::Thrift::Struct_Union

        FIELDS = {

        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class CurrentTrendsDefault_result
        include ::Thrift::Struct, ::Thrift::Struct_Union
        SUCCESS = 0

        FIELDS = {
          SUCCESS => {:type => ::Thrift::Types::LIST, :name => 'success', :element => {:type => ::Thrift::Types::STRUCT, :class => ::TrendServer::Gen::TrendResult}}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class CurrentTrends_args
        include ::Thrift::Struct, ::Thrift::Struct_Union
        SPANINSECONDS = 1
        MINOCCURRENCE = 2
        MINLENGTH = 3
        MAXLENGTH = 4
        TOP = 5
        DROPBLACKLISTED = 6
        ONLYWHITELISTED = 7

        FIELDS = {
          SPANINSECONDS => {:type => ::Thrift::Types::I32, :name => 'spanInSeconds'},
          MINOCCURRENCE => {:type => ::Thrift::Types::DOUBLE, :name => 'minOccurrence'},
          MINLENGTH => {:type => ::Thrift::Types::I32, :name => 'minLength'},
          MAXLENGTH => {:type => ::Thrift::Types::I32, :name => 'maxLength'},
          TOP => {:type => ::Thrift::Types::I32, :name => 'top'},
          DROPBLACKLISTED => {:type => ::Thrift::Types::BOOL, :name => 'dropBlacklisted'},
          ONLYWHITELISTED => {:type => ::Thrift::Types::BOOL, :name => 'onlyWhitelisted'}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class CurrentTrends_result
        include ::Thrift::Struct, ::Thrift::Struct_Union
        SUCCESS = 0

        FIELDS = {
          SUCCESS => {:type => ::Thrift::Types::LIST, :name => 'success', :element => {:type => ::Thrift::Types::STRUCT, :class => ::TrendServer::Gen::TrendResult}}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class TrendsEndingAt_args
        include ::Thrift::Struct, ::Thrift::Struct_Union
        UNIXENDATTIME = 1
        SPANINSECONDS = 2
        MINOCCURRENCE = 3
        MINLENGTH = 4
        MAXLENGTH = 5
        TOP = 6
        DROPBLACKLISTED = 7
        ONLYWHITELISTED = 8

        FIELDS = {
          UNIXENDATTIME => {:type => ::Thrift::Types::I32, :name => 'unixEndAtTime'},
          SPANINSECONDS => {:type => ::Thrift::Types::I32, :name => 'spanInSeconds'},
          MINOCCURRENCE => {:type => ::Thrift::Types::DOUBLE, :name => 'minOccurrence'},
          MINLENGTH => {:type => ::Thrift::Types::I32, :name => 'minLength'},
          MAXLENGTH => {:type => ::Thrift::Types::I32, :name => 'maxLength'},
          TOP => {:type => ::Thrift::Types::I32, :name => 'top'},
          DROPBLACKLISTED => {:type => ::Thrift::Types::BOOL, :name => 'dropBlacklisted'},
          ONLYWHITELISTED => {:type => ::Thrift::Types::BOOL, :name => 'onlyWhitelisted'}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class TrendsEndingAt_result
        include ::Thrift::Struct, ::Thrift::Struct_Union
        SUCCESS = 0

        FIELDS = {
          SUCCESS => {:type => ::Thrift::Types::LIST, :name => 'success', :element => {:type => ::Thrift::Types::STRUCT, :class => ::TrendServer::Gen::TrendResult}}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class StringToWords_args
        include ::Thrift::Struct, ::Thrift::Struct_Union
        STRINGTOANALYZE = 1

        FIELDS = {
          STRINGTOANALYZE => {:type => ::Thrift::Types::STRING, :name => 'stringToAnalyze'}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class StringToWords_result
        include ::Thrift::Struct, ::Thrift::Struct_Union
        SUCCESS = 0

        FIELDS = {
          SUCCESS => {:type => ::Thrift::Types::LIST, :name => 'success', :element => {:type => ::Thrift::Types::STRING}}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class StoreString_args
        include ::Thrift::Struct, ::Thrift::Struct_Union
        STRINGTOSTORE = 1
        UNIXCREATEDATTIME = 2
        WEEKSAGODATATOEXPIRE = 3

        FIELDS = {
          STRINGTOSTORE => {:type => ::Thrift::Types::STRING, :name => 'stringToStore'},
          UNIXCREATEDATTIME => {:type => ::Thrift::Types::I32, :name => 'unixCreatedAtTime'},
          WEEKSAGODATATOEXPIRE => {:type => ::Thrift::Types::I32, :name => 'weeksAgoDataToExpire'}
        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

      class StoreString_result
        include ::Thrift::Struct, ::Thrift::Struct_Union

        FIELDS = {

        }

        def struct_fields; FIELDS; end

        def validate
        end

        ::Thrift::Struct.generate_accessors self
      end

    end

  end
end
