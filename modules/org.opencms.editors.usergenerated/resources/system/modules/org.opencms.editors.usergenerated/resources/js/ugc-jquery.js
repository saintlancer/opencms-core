 function UGC () {
    // the id of the form in the HTML
    this.formId = arguments[0];
    
    // the mappings from form field id's to XML content xpath
    this.mappings = {};

    // UCG session
    this.session = null;
    // the content as it was provided from the server
    this.content = null;    

    // a copy / clone of the content, used for the modified result
    this.contentClone = null;
    
    this.OPTIONAL = "OPTIONAL";
    this.UPLOAD = "UPLOAD";
    
    // element that directly returns the form DOM element after initialization
    this.form = null;
 }
 
function UGCMapping (ugc, args) {
    // this is the main mapping object that maps a form Id to a content xpath
    this.formId = args[0];
    this.contentPath = args[1];
    this.isUpload = false;
    this.deleteEmptyValue = false;
    this.notEmpty = false;
    if (args.length > 1) {
        // check the additional arguments for further options
        for (i = 2; i < args.length; i++) {
            if (ugc.UPLOAD === args[i]) {
                // mark this as an upload field, which means we don't fill it automatically in the form and content             
                this.isUpload = true;               
            } else if (ugc.OPTIONAL === args[i]) {
                // decides if an empty value is actually deleted in the modified content, 
                // or kept as an empty string (the default)
                // use this for optional values in the XML content that should be fully removed
                // if only an empty string is provided in the form
                this.deleteEmptyValue = true;
            }
            // else { alert("Ignoring: " + args[i]); }
        }
    }
}

UGC.prototype.map = function() {
    // add a new mapping
    var mapping = new UGCMapping(this, arguments);
    // this.debugMap(mapping);
    this.mappings[arguments[0]] = mapping;
}
 
UGC.prototype.debugMap = function(mapping) {
    alert("Xpath: " + mapping.contentPath + "\nFormId: " +  mapping.formId + "\nDeleteEmpty: " + mapping.deleteEmptyValue + "\nUpload: " + mapping.isUpload);
}       

UGC.prototype.debugContent = function(contentArray) {
    var result = "";
    for (var key in contentArray) {
        if (contentArray.hasOwnProperty(key)) {
            var value = contentArray[key];
            result += "Xpath: " + key + "\nValue: " + value + "\n\n";
        }
    }
    alert(result);
}

 UGC.prototype.getForm = function() {
    // check if we have one or more arguments
    if (arguments.length == 1) {
        // lookup the form element with the given name
        return $("#" + this.formId + " :input[name='" + arguments[0] + "']");
    } else if (arguments.length == 0) {
        // zero arguments, return the complete form
        var theForm = $("#" + this.formId);
        if (this.form == null) {
            // set the form DOM access element 
            this.form = theForm[0];     
        }
        return theForm;
    }
    // no argument returns null
    return null;
}; 

UGC.prototype.getFormVal = function(name) {
    // lookup the value from the form element with the given name
    return this.getForm(name).val();
};

UGC.prototype.getXpath = function() {
    // check if we have one or more arguments
    if (arguments.length == 1) {
        return this.mappings[arguments[0]].contentPath;
    }
    return null;
}

 UGC.prototype.formHas = function() {
    // check if we have one or more arguments
    if (arguments.length == 1) {
        // single value: lookup the value from the form element with the given name
        return this.getFormVal(arguments[0]).trim().length > 0;
    } else {
        // iterate the array of arguments and check for all with shout-circuit
        for (i = 0; i < arguments.length; i++) {    
            if (this.formHasNot(arguments[i])) {
                return false;
            }
        }
        return true;
    }
    // in case of no arguments at all
    return false;
};

 UGC.prototype.formHasNot = function() {
    // check if we have one or more arguments
    if (arguments.length == 1) {
        // single value: lookup the value from the form element with the given name
        return this.getFormVal(arguments[0]).trim().length <= 0;
    } else {
        // iterate the array of arguments and check for all with shout-circuit
        for (i = 0; i < arguments.length; ++i) {
            if (this.formHas(arguments[i])) {
                return false;
            }
        }   
        return true;
    }
    // in case of no arguments at all
    return false;
};

 UGC.prototype.formHasOne = function() {
    // check if we have one or more arguments
    if (arguments.length == 1) {
        // single value: lookup the value from the form element with the given name
        return this.formHas(arguments[0]);
    } else {
        // iterate the array of arguments and check for all with shout-circuit
        for (i = 0; i < arguments.length; ++i) {
            if (this.formHas(arguments[i])) {
                return true;
            }
        }   
        return false;
    }
    // in case of no arguments at all
    return false;
};

 UGC.prototype.contentHas = function() {
    // check if we have one or more arguments
    if (arguments.length == 1) {
        // single value: lookup the value from the content array
        var value = this.getContent(arguments[0]);
        return (typeof value === "undefined") ? false : true;
    } else {
        // iterate the array of arguments and check for all with shout-circuit
        for (i = 0; i < arguments.length; i++) {    
            if (this.contentHasNot(arguments[i])) {
                return false;
            }
        }
        return true;
    }
    // in case of no arguments at all
    return false;
};

 UGC.prototype.contentHasNot = function() {
    // check if we have one or more arguments
    if (arguments.length == 1) {
        // single value: lookup the value from the content array
        var value = this.getContent(arguments[0]);
        return (typeof value === "undefined") ? true : false;
    } else {
        // iterate the array of arguments and check for all with shout-circuit
        for (i = 0; i < arguments.length; i++) {    
            if (this.contentHas(arguments[i])) {
                return false;
            }
        }
        return true;
    }
    // in case of no arguments at all
    return false;
};

 UGC.prototype.contentHasOne = function() {
    // check if we have one or more arguments
    if (arguments.length == 1) {
        // single value: lookup the value from the content array
        return this.contentHas(arguments[0]);
    } else {
        // iterate the array of arguments and check for all with shout-circuit
        for (i = 0; i < arguments.length; ++i) {
            if (this.contentHas(arguments[i])) {
                return true;
            }
        }   
        return false;
    }
    // in case of no arguments at all
    return false;
};

 UGC.prototype.setForm = function() {
    // check if the name parameter was provided, if not we have to initialize everything later
    if (this.content != null) {
        if (arguments.length == 1) {
            // set the form element with the given name to the content value stored in the mapping with the same name
            this.getForm(arguments[0]).val(this.content[this.mappings[arguments[0]].contentPath]);
        } else {
            // fill the complete form with all mapped values
            this.fillForm();
        }
    }
};

 UGC.prototype.setContent = function() {
    // check if the name parameter was provided, if not we have to initialize everything later
    var formId = (arguments.length > 0) ? arguments[0] : null;
    if (this.contentClone == null) {
        // initialize the content clone on first call
        this.createContentClone();  
    }
    if (formId != null) {
        // set the content (clone) value stored in the mapping with the given name to the form element value with the same 
        var value = (arguments.length > 1) ? arguments[1] : this.getFormVal(formId);
        var mapping = this.mappings[formId];
        this.contentClone[mapping.contentPath] = value;
        if (value.trim().length <= 0) {
            if (mapping.deleteEmptyValue) {
                this.contentClone[mapping.contentPath] = null;
            }       
        }
    } else {
        // no form id provided, set the complete content with all mapped values
        this.fillContent();     
    }
};

 UGC.prototype.deleteContent = function(name) {
    if (this.contentClone != null) {
        // delete the content (clone) value stored in the mapping with the given name
        this.contentClone[this.mappings[name].contentPath] = null;
    }
};

 UGC.prototype.deleteParentContent = function(name) {
    if (this.contentClone != null) {
        // delete the content (clone) value stored in the mapping with the given name
        var xpath = this.mappings[name].contentPath;
        var parentName = xpath.substring(0, xpath.lastIndexOf("/"));
        for (var key in this.contentClone) {
            if (this.content.hasOwnProperty(key) && (key.indexOf(parentName) == 0)) {
                delete this.contentClone[key];
            }
        }
        this.contentClone[parentName] = null;
    }
};

 UGC.prototype.getContent = function() {
    // check if we have one or more arguments
    if (arguments.length == 1) {
        // return the selected value from the original content array
        return this.contentClone[this.getXpath(arguments[0])];
    } 
    // return the content complete clone map
    return this.contentClone;
};

UGC.prototype.createContentClone = function() {
    // initialize the clone array
    this.contentClone = {}; 
    if (this.content != null) {
        // copy all the elements from the content to the clone
        for (var key in this.content) {
            if (this.content.hasOwnProperty(key)) {
                this.contentClone[key] = this.content[key];
            }
        }
    }
    // return the generated clone
    return this.contentClone;
}

UGC.prototype.fillForm = function() {
    // iterate over all mappings, fill the form with the mapped values
    for (var key in this.mappings) {
        var mapping = this.mappings[key];
        if (!mapping.isUpload) { 
            this.setForm(mapping.formId);
        } // else { alert("Ignoring: " + mapping.contentPath); }
    }
}

UGC.prototype.fillContent = function() {
    // iterate over all mappings, fill the content with values from the mapped form elements
    for (var key in this.mappings) {
        var mapping = this.mappings[key];   
        if (!mapping.isUpload) { 
            this.setContent(mapping.formId);
        } // else { alert("Ignoring: " + mapping.contentPath); }
    }
}

UGC.prototype.setSession = function() {
    // set the session to the provided parameter
    this.session = arguments[0];
    // initialize the content from the session
    this.content = this.session.getValues();
};

UGC.prototype.getSession = function() {
    // wrapper for session
    return this.session;
};

UGC.prototype.destroySession = function() {
    // wrapper for session
    if (this.session != null) this.session.destroy();
};