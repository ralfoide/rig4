pub struct HashStore {
    v: i32
}

impl HashStore {
    pub fn new(v: i32) -> HashStore {        
        println!("new hash store");
        HashStore{ v }
    }

    pub fn v(&self) {
        println!("hash_store.v( {} )", self.v);
    }
}

#[cfg(test)]
mod tests_hash_store {
    #[test]
    fn test1() {
        assert_eq!(2 + 2, 4);
    }
}
