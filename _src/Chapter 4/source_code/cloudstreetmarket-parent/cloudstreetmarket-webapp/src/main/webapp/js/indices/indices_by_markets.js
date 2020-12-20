cloudStreetMarketApp.factory("indicesByMarketTableFactory", function ($http) {
    return {
        get: function (market, ps, pn, sf, sd) {
        	return $http.get("/api/indices/"+market+".json?size="+ps+"&page="+pn+"&sort="+sf+","+sd);
        }
    }
});

cloudStreetMarketApp.controller('indicesByMarketTableController', function PaginationCtrl($scope, indicesByMarketTableFactory, $routeParams){
	  $scope.loadPage = function () {
		indicesByMarketTableFactory.get($routeParams.name, $scope.pageSize, $scope.currentPage, $scope.sortedField, $scope.sortDirection)
			.success(function(data, status, headers, config) {
				updatePaginationIndicesBM ($scope, data);
	        });
	  }

	  $scope.market = $routeParams.name;
	  
	  //Pagination
	  $scope.setPage = function (pageNo) {
		  $scope.currentPage = pageNo-1
		  $scope.loadPage()
	  }
	  
	  //Sorting
	  $scope.setSort = function(field) {
		  updateSortParamIndicesBM ($scope, field)
	  }

	  //Init.
	  initIndicesBM($scope);
});

/*
 * Static methods
 */
function updatePaginationIndicesBM ($scope, data){
	$scope.indicesForMarket = data.content;
    $scope.currentPage = data.number;
    $scope.paginationCurrentPage = data.number+1;
    $scope.paginationTotalItems =  data.totalElements;
}

function updateSortParamIndicesBM ($scope, field){
	  if( $scope.sortedField == field){
		  $scope.sortDirection = ($scope.sortDirection === "asc")? "desc" : "asc";
	  }
	  else{
		  $scope.sortDirection = "asc";
	  }
	  $scope.sortedField = field;
	  $scope.loadPage();
}

function initIndicesBM ($scope){
	$scope.sortedField = "name";
	$scope.sortDirection = "asc";
	
	$scope.maxSize = 6; //number of visible buttons for pages
	$scope.currentPage = 0;
	$scope.paginationTotalItems = 10;
	$scope.paginationCurrentPage = 1;
	$scope.pageSize = 5;
	$scope.indicesForMarket = [];
	$scope.loadPage();
}

