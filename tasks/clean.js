var del = require('del');

module.exports = function(config){
  return function(forTask){
    del(config.clean[forTask]);
  };
};