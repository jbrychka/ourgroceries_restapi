@RestController
@RequestMapping("/ourgroceries")
public class OurGroceriesController {

    @Autowired
    private OurGroceriesService ourGroceriesService;

    @PostMapping("/add")
    public ResponseEntity<String> addItem(@RequestParam String email, @RequestParam String password,
                                          @RequestParam String listId, @RequestParam String item) {
        try {
            String response = ourGroceriesService.addItem(email, password, listId, item);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
