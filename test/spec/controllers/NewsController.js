// Generated by CoffeeScript 1.6.3
(function() {
  'use strict';
  describe('Controller: NewsCtrl', function() {
    var NewsCtrl, scope;
    beforeEach(module('app'));
    NewsCtrl = {};
    scope = {};
    beforeEach(inject(function($controller, $rootScope) {
      return NewsCtrl = $controller('NewsCtrl', {
        $stateParams: {
          news: 2
        }
      });
    }));
    return it('should attach a list of awesomeThings to the scope', function() {
      return expect(NewsCtrl.news.id).toBe(2);
    });
  });

}).call(this);