
#![allow(non_snake_case)]

use std::collections::HashMap;

pub struct HashStore {
    mCache: HashMap<String, String>
}

impl HashStore {
    pub fn new() -> HashStore {
        HashStore{
            mCache: HashMap::new()
        }
    }

    pub fn putString(&mut self, description: &str, content: &str) {
        self.mCache.insert(String::from(description), String::from(content));
    }

    pub fn getString(&self, description: &str) -> Option<&String> {
        self.mCache.get(description)
    }
}

#[cfg(test)]
mod tests_hash_store {
    use super::*;

    #[test]
    fn test_putString_getString() {
        let mut hs = HashStore::new();
        assert_eq!(hs.getString("key"), None);

        hs.putString("key", "value");
        assert_eq!(hs.getString("key").unwrap(), "value");
        assert_eq!(hs.getString("key").unwrap(), "value");

        hs.putString("key", "value2");
        assert_eq!(hs.getString("key").unwrap(), "value2");
    }
}
